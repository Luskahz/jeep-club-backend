package com.jeepclub.backend.storage.infra.image;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Bounded JPEG APP1 TIFF orientation parsing; malformed metadata is rejected, never used as an offset unchecked. */
final class ExifOrientation {
    static int read(byte[] bytes) {
        if(bytes.length>=8 && (bytes[0]&255)==137 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G') {
            int pos=8;
            while(pos+12<=bytes.length) {
                long length=Integer.toUnsignedLong(ByteBuffer.wrap(bytes,pos,4).getInt());
                if(length>bytes.length-pos-12) throw new IllegalArgumentException("Invalid PNG metadata");
                if(bytes[pos+4]=='e' && bytes[pos+5]=='X' && bytes[pos+6]=='I' && bytes[pos+7]=='f') {
                    var crc=new java.util.zip.CRC32();crc.update(bytes,pos+4,(int)length+4);
                    long stored=Integer.toUnsignedLong(ByteBuffer.wrap(bytes,pos+8+(int)length,4).getInt());
                    if(crc.getValue()!=stored) throw new IllegalArgumentException("Invalid PNG EXIF checksum");
                    return tiff(bytes,pos+8,(int)length);
                }
                pos+=(int)length+12;
            }
            return 1;
        }
        if(bytes.length<4 || (bytes[0]&255)!=255 || (bytes[1]&255)!=216) return 1;
        int pos=2;
        while(pos+4<=bytes.length) {
            if((bytes[pos]&255)!=255) throw new IllegalArgumentException("Invalid JPEG segment");
            int marker=bytes[pos+1]&255;pos+=2;
            if(marker==0xda || marker==0xd9) return 1;
            if(marker==0x01 || marker>=0xd0 && marker<=0xd7) continue;
            int size=((bytes[pos]&255)<<8)|(bytes[pos+1]&255);
            if(size<2 || (long)pos+size>bytes.length) throw new IllegalArgumentException("Invalid JPEG metadata");
            if(marker==0xe1 && size>=16 && bytes[pos+2]=='E' && bytes[pos+3]=='x' && bytes[pos+4]=='i' && bytes[pos+5]=='f' && bytes[pos+6]==0 && bytes[pos+7]==0)
                return tiff(bytes,pos+8,size-8);
            pos+=size;
        }
        return 1;
    }
    private static int tiff(byte[] bytes,int start,int size) {
        var b=ByteBuffer.wrap(bytes,start,size).slice();
        if(size<8) throw new IllegalArgumentException("Invalid EXIF");
        int signature=b.getShort(0)&65535;
        if(signature==0x4949) b.order(ByteOrder.LITTLE_ENDIAN);
        else if(signature!=0x4d4d) throw new IllegalArgumentException("Invalid EXIF order");
        if((b.getShort(2)&65535)!=42) throw new IllegalArgumentException("Invalid EXIF header");
        long offset=Integer.toUnsignedLong(b.getInt(4));
        if(offset<8 || offset+2>size) throw new IllegalArgumentException("Invalid EXIF offset");
        int count=b.getShort((int)offset)&65535;
        if(offset+2L+count*12L>size) throw new IllegalArgumentException("Invalid EXIF directory");
        for(int i=0;i<count;i++) {
            int at=(int)offset+2+i*12;
            if((b.getShort(at)&65535)==0x112) {
                if((b.getShort(at+2)&65535)!=3 || b.getInt(at+4)!=1) throw new IllegalArgumentException("Invalid orientation type");
                int orientation=b.getShort(at+8)&65535;
                if(orientation<1 || orientation>8) throw new IllegalArgumentException("Invalid orientation");
                return orientation;
            }
        }
        return 1;
    }
    static BufferedImage apply(BufferedImage source,int orientation) {
        if(orientation==1) return source;
        int w=source.getWidth(),h=source.getHeight();boolean swap=orientation>=5;
        var target=new BufferedImage(swap?h:w,swap?w:h,source.getColorModel().hasAlpha()?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int tx=x,ty=y;
            switch(orientation) {
                case 2 -> tx=w-1-x;
                case 3 -> {tx=w-1-x;ty=h-1-y;}
                case 4 -> ty=h-1-y;
                case 5 -> {tx=y;ty=x;}
                case 6 -> {tx=h-1-y;ty=x;}
                case 7 -> {tx=h-1-y;ty=w-1-x;}
                case 8 -> {tx=y;ty=w-1-x;}
                default -> throw new IllegalArgumentException("Invalid orientation");
            }
            target.setRGB(tx,ty,source.getRGB(x,y));
        }
        return target;
    }
    private ExifOrientation() {}
}
