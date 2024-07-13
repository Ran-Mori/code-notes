## 坐标

* `left, top, right, bottom, elevation` - The distance in pixels from the xxx edge of this view's parent
* `translateX, translateY, translateZ` - The x/y/z location of this view relative to its left/top/elevation position
* `x/y/z = mLeft/mTop/elevation + translationX/translationY/translationZ` - The visual x/y/z position of this view, in pixels

## translate value

* 设置`translate`后只重新`draw`，不会重新进行`measure, layout`
* 是一种低代价改变`View`位置的参数，因为不用`measure、layout`，常用来做动画
* 设置后重新调用`requestLayout`依然不会使`translate`失效