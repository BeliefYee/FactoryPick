import { categoryIconSource } from '../lib/categoryIcons'

export default function CategoryIcon({ category = '미분류', size = 26 }) {
  return <img className="category-icon" src={categoryIconSource(category)} width={size} height={size} alt="" aria-hidden="true" />
}
