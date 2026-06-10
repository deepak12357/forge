create table edge (
    id bigserial primary key,

    repo_id bigint not null,

    from_method_node_id bigint,

    to_method_node_id bigint,

    type varchar(50) not null,

    metadata text,

    constraint fk_edge_repo
        foreign key (repo_id)
        references repo(id),

    constraint fk_edge_from_method
        foreign key (from_method_node_id)
        references method_node(id),

    constraint fk_edge_to_method
        foreign key (to_method_node_id)
        references method_node(id)
);

create index idx_edge_from_method on edge(from_method_node_id);
create index idx_edge_to_method on edge(to_method_node_id);

