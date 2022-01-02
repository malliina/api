create table push_clients
(
    id     bigint auto_increment primary key,
    token  varchar(191) unique         not null,
    device varchar(128)                not null,
    added  timestamp(3) default now(3) not null
);
