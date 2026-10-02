package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "publication_notice_history")
@PrimaryKeyJoinColumn(name = "history_id")
public class NoticeHistoryEntity extends PublicationHistoryEntity { }
