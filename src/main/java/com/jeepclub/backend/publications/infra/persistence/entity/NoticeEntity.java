package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "publication_notices")
@PrimaryKeyJoinColumn(name = "publication_id")
public class NoticeEntity extends PublicationEntity { }
