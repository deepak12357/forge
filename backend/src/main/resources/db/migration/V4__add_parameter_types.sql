-- Add parameter types to method_node to support precise overload matching
alter table method_node add column parameter_types varchar(2000);

