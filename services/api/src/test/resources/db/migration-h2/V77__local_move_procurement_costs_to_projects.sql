update project_projects project
set actual_cost = coalesce(project.actual_cost, 0) + coalesce((
  select sum(allocation.amount)
  from procurement_cost_allocations allocation
  join procurement_goods_receipts receipt on receipt.id = allocation.receipt_id
  where allocation.project_id = project.id
    and allocation.cost_type = 'PROJECT'
    and not exists (
      select 1
      from project_cost_entries entry
      where entry.project_id = allocation.project_id
        and entry.source_type = 'PROCUREMENT'
        and entry.source_no = receipt.code
    )
), 0)
where exists (
  select 1
  from procurement_cost_allocations allocation
  join procurement_goods_receipts receipt on receipt.id = allocation.receipt_id
  where allocation.project_id = project.id
    and allocation.cost_type = 'PROJECT'
    and not exists (
      select 1
      from project_cost_entries entry
      where entry.project_id = allocation.project_id
        and entry.source_type = 'PROCUREMENT'
        and entry.source_no = receipt.code
    )
);

insert into project_cost_entries (
  id,
  tenant_id,
  project_id,
  category,
  source_type,
  source_no,
  description,
  amount,
  incurred_date,
  created_at,
  updated_at,
  created_by,
  updated_by,
  version
)
select
  allocation.id,
  allocation.tenant_id,
  allocation.project_id,
  'MATERIAL',
  'PROCUREMENT',
  receipt.code,
  concat('Procurement receipt: ', allocation.part_name),
  allocation.amount,
  allocation.incurred_date,
  allocation.created_at,
  allocation.updated_at,
  allocation.created_by,
  allocation.updated_by,
  allocation.version
from procurement_cost_allocations allocation
join procurement_goods_receipts receipt on receipt.id = allocation.receipt_id
where allocation.cost_type = 'PROJECT'
  and allocation.project_id is not null
  and not exists (
    select 1
    from project_cost_entries entry
    where entry.project_id = allocation.project_id
      and entry.source_type = 'PROCUREMENT'
      and entry.source_no = receipt.code
  );
