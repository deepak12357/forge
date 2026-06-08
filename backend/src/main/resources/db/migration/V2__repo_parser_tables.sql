create table repo (
    id bigserial primary key,

    git_url varchar(1000) not null,

    status varchar(50) not null,

    class_count integer not null default 0,

    method_count integer not null default 0,

    parse_failure_count integer not null default 0,

    created_at timestamptz not null,

    updated_at timestamptz not null
);

create table class_node (
    id bigserial primary key,

    repo_id bigint not null,

    package_name varchar(500),

    class_name varchar(255) not null,

    fully_qualified_name varchar(1000) not null,

    file_path varchar(2000) not null,

    start_line integer,

    end_line integer,

    constraint fk_class_node_repo
        foreign key (repo_id)
        references repo(id)
);

create table method_node (
    id bigserial primary key,

    class_node_id bigint not null,

    method_name varchar(255) not null,

    signature varchar(1000) not null,

    return_type varchar(500),

    modifiers varchar(500),

    start_line integer,

    end_line integer,

    constraint fk_method_node_class
        foreign key (class_node_id)
        references class_node(id)
);