package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "publication_services")
@PrimaryKeyJoinColumn(name = "publication_id")
public class ServicePublicationEntity extends PublicationEntity { }
