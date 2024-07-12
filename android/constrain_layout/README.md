## bias

* 计算公式: `bias = layout_constraintTop_toTopOf / (layout_constraintBottom_toBottomOf + layout_constraintBottom_toBottomOf)`
* 默认值是0.5，即此时`layout_constraintTop_toTopOf` = `layout_constraintBottom_toBottomOf`

## chainStyle

* 设置在什么地方: on the first element of a chain
* behavior
  1. spread - 默认值，全部平分
  2. spread_inside - 第一个`layout_constraintTop_toTopOf`和最后一个`layout_constraintBottom_toBottomOf` 为零，其余平分
  3. packed - 第一个`layout_constraintTop_toTopOf`和最后一个`layout_constraintBottom_toBottomOf` 平均，其余为零
