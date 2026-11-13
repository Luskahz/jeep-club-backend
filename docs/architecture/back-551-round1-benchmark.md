# BACK-551 — medição local do upload síncrono

Em 12/11/2026, código de produção `edbbeed7a80b6b6313c474565acb333c7fb240e1`, com o teste de benchmark adicionado neste commit. Java Microsoft 17.0.18, Windows 10, 16 processadores lógicos reportados pela JVM, Spring Boot/MockMvc, banco H2 de teste e filesystem temporário local. O codec WebP nativo estava disponível.

Comando: `mvnw.cmd -Dtest=StorageHttpFlowTest#benchmarkConcurrentProfileUploads -Dstorage.benchmark=true test`, com `JAVA_HOME` do processo apontando para Java 17. Saída local: `target/back-551-benchmark.log`.

Foram gerados 22 PNGs distintos de pixels pseudoaleatórios, 1024×768 e aproximadamente 2.361.711 bytes cada. Dois serviram de aquecimento; os 20 demais foram enviados por duas threads, sem retry, com o mesmo usuário de fixture. A medida começa imediatamente antes de cada chamada MockMvc e termina após a resposta 201, abrangendo HTTP simulado, autorização, admissão, decodificação, sanitização, variantes, custódia física e transações. A espera na fila do executor antes de a thread começar não integra a duração. Todos os 22 uploads retornaram 201.

Resultado: **p95 263 ms**, máximo **266 ms**, 20 amostras medidas; o teste passou no alvo de 10.000 ms nesse cenário. O valor é uma observação local, não representa p95 de produção: não inclui rede real, concorrência acima de dois, MySQL, filesystem de produção, outras dimensões/formatos ou operação multi-instância. A amostra é pequena e deve ser repetida com cargas representativas antes de qualquer ativação. Os tempos não provam aprovação da suíte global nem CI.
