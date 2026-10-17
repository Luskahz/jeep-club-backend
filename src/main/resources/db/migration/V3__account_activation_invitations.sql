-- Reference: PR #95 / 6865bc4. No invitation tokens are backfilled or generated.

create table authentication_account_activation_invitations (
    expires_at datetime(6) not null,
    identity_id bigint not null,
    used_at datetime(6),
    version bigint not null,
    delivery_status varchar(24) not null,
    token_hash varchar(64) not null,
    primary key (identity_id)
) engine=InnoDB;

alter table authentication_account_activation_invitations add constraint UKaadjc236y8nqhlaay4fl6qxpk unique (token_hash);
