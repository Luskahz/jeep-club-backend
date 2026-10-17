-- Reference: PR #93 / bde973f. Add optional profile without rewriting identity data.

create table identity_user_profiles (
    state varchar(2),
    postal_code varchar(8),
    user_id bigint not null,
    house_number varchar(20),
    city varchar(100),
    neighborhood varchar(100),
    complement varchar(150),
    occupation varchar(150),
    street varchar(150),
    workplace varchar(150),
    primary key (user_id)
) engine=InnoDB;

alter table identity_user_profiles add constraint fk_identity_user_profiles_user foreign key (user_id) references identity_users (id);
