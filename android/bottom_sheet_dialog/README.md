## layout

* design_bottom_sheet_dialog.xml

  ```xml
  <!--design_bottom_sheet_dialog.xml-->
  <FrameLayout android:id="@+id/container">
    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:id="@+id/coordinator">
      <View android:id="@+id/touch_outside" />
      <FrameLayout android:id="@+id/design_bottom_sheet"/>
    </androidx.coordinatorlayout.widget.CoordinatorLayout>
  </FrameLayout>       
  ```

* 核心是`CoordinatorLayout`，实现在`com.google.android.material.bottomsheet.BottomSheetDialog`
