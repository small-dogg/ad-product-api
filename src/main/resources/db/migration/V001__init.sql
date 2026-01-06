create table product (
                         id            bigint primary key,
                         partner_id    bigint not null,
                         name          varchar(200) not null,
                         status        varchar(30) not null,
                         price         bigint not null,
                         image_url     varchar(500),
                         created_at    timestamp not null,
                         modified_at   timestamp not null
);

create index idx_product_partner_id
    on product (partner_id);

create index idx_product_partner_status
    on product (partner_id, status);

create index idx_product_created_at
    on product (created_at desc);
