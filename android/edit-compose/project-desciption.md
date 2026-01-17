## Background:

1. My application's homepage is expected to have 5 tabs at the bottom: [Adjust], [Filter], [Crop], [AI Erase], and [Create]. You can view the ASCII layout in edit-adjust-tab.txt.

2. The UI layout of the first tab on the homepage - [Adjust] - is described in ASCII code in edit-adjust-tab.txt.

3. The UI layout of the second tab on the homepage - [Crop] - is described in ASCII code in edit-crop-tab.txt.

4. Clicking different tabs at the bottom of the homepage will switch to different pages, such as the [Filter] page or the [Crop] page.

5. The bottom bar is hidden at the bottom of [Crop]. Clicking "back" returns to the previous tab. If the returned tab is the [Adjust] page, the bottom bar is displayed.

## Requirements:

1. Implement the above UI requirements using Jetpack Compose.

2. Use a highly cohesive and loosely coupled architecture.

3. Use the MVI paradigm for data-driven UI.

4. Follow standard specifications and avoid haphazard implementation.