import FilterSelect from './FilterSelect'

export default function CategorySelect({ value, categories, onChange }) {
  return <FilterSelect value={value} options={categories} onChange={onChange}
    label="업종 카테고리" emptyLabel="전체 카테고리" />
}
