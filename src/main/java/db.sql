create table man10_market.exchange_history
(
    id          int unsigned auto_increment
        primary key,
    item_id     int         null,
    uuid_buyer  varchar(40) null,
    buyer       varchar(20) null,
    uuid_seller varchar(40) null,
    seller      varchar(20) null,
    amount      bigint      null,
    price       double      null,
    total_price double      null,
    datetime    datetime    null,
    year        int         null,
    month       int         null,
    day         int         null,
    hour        int         null,
    min         int         null
)
    charset = utf8;

create table man10_market.history_day
(
    id      int unsigned auto_increment
        primary key,
    item_id int    null,
    high    double null,
    low     double null,
    open    double null,
    close   double null,
    volume  int    null,
    year    int    null,
    month   int    null,
    day     int    null,
    hour    int    null,
    min     int    null
)
    charset = utf8;

create table man10_market.history_hour
(
    id      int unsigned auto_increment
        primary key,
    item_id int    null,
    high    double null,
    low     double null,
    open    double null,
    close   double null,
    volume  int    null,
    year    int    null,
    month   int    null,
    day     int    null,
    hour    int    null,
    min     int    null
)
    charset = utf8;

create table man10_market.ipay_log
(
    id        int unsigned auto_increment
        primary key,
    from_uuid varchar(40) null,
    from_name varchar(20) null,
    to_uuid   varchar(40) null,
    to_name   varchar(20) null,
    item_id   int         null,
    amount    int         null
)
    charset = utf8;

create table man10_market.item_index
(
    id            int unsigned auto_increment
        primary key,
    uuid          varchar(40)  default '' not null,
    player        varchar(20)  default '' not null,
    item_key      varchar(256) default '' not null,
    name          varchar(256)            not null,
    type          varchar(256)            not null,
    durability    int                     null,
    price         double                  not null,
    initial_price double                  not null,
    last_price    double                  not null,
    tick          double                  null,
    datetime      datetime                null,
    sell          int          default 0  null,
    buy           int          default 0  null,
    base64        longtext                null,
    max_price     double                  null,
    min_price     double                  null,
    bid           double                  null,
    ask           double                  null,
    disabled      tinyint(1)   default 0  null,
    lot           int                     null
)
    charset = utf8;

create table man10_market.item_storage
(
    id       int unsigned auto_increment
        primary key,
    uuid     varchar(40)  null,
    player   varchar(20)  null,
    item_id  int          not null,
    `key`    varchar(256) null,
    amount   bigint       null,
    datetime datetime     null
)
    charset = utf8;

create table man10_market.order_history
(
    id int unsigned auto_increment
        primary key
)
    charset = utf8;

create table man10_market.order_tbl
(
    id             int unsigned auto_increment
        primary key,
    item_id        int                     not null,
    `key`          varchar(256) default '' not null,
    uuid           varchar(40)  default '' not null,
    player         varchar(20)  default '' not null,
    price          double                  not null,
    amount         int                     null,
    initial_amount int                     null,
    buy            tinyint(1)              null,
    datetime       datetime                null
)
    charset = utf8;

create table man10_market.price_history
(
    id       int unsigned auto_increment
        primary key,
    item_id  int      null,
    price    double   null,
    datetime datetime null
)
    charset = utf8;

create table man10_market.sign_location
(
    id    int auto_increment
        primary key,
    world varchar(256) null,
    x     double       null,
    y     double       null,
    z     double       null
)
    charset = utf8;

create table man10_market.transaction_log
(
    id          int unsigned auto_increment
        primary key,
    item        varchar(40)  null,
    order_id    int          null,
    uuid        varchar(40)  null,
    target_uuid varchar(40)  null,
    player      varchar(20)  null,
    action      varchar(32)  null,
    price       double       null,
    amount      int          null,
    world       varchar(256) null,
    x           double       null,
    y           double       null,
    z           double       null,
    datetime    datetime     null
)
    charset = utf8;

create table man10_market.user_assets_history
(
    id                  int unsigned auto_increment
        primary key,
    uuid                varchar(40) default '' not null,
    player              varchar(20) default '' not null,
    balance             double                 not null comment '残額',
    estimated_valuation double                 null comment '総見積もり資産',
    total_amount        bigint                 null comment '総アイテム数',
    year                int                    null,
    month               int                    null,
    day                 int                    null,
    itemlist            text                   null
)
    charset = utf8;

create table man10_market.user_index
(
    id            int unsigned auto_increment
        primary key,
    uuid          varchar(40) null,
    name          varchar(20) null,
    company       int         null,
    company_owner varchar(40) null,
    balance       double      null,
    `lock`        tinyint(1)  null,
    constraint uuid
        unique (uuid)
)
    charset = utf8;

