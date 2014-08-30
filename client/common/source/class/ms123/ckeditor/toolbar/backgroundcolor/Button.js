/**
 * Copyright (c) 2011 Derrell Lipman
 * 
 * License:
 *   LGPL: http://www.gnu.org/licenses/lgpl.html 
 *   EPL : http://www.eclipse.org/org/documents/epl-v10.php
 */

qx.Class.define("ms123.ckeditor.toolbar.backgroundcolor.Button", {
	extend: ms123.ckeditor.toolbar.AbstractToolbarEntry,
	include: ms123.ckeditor.toolbar.backgroundcolor.MAction,

	construct: function (_ckrte) {
		var control;

		// Call the superclass constructor
		this.base(arguments, _ckrte);

		// Instantiate the control.
		control = new qx.ui.form.Button(null, "rte/toolbar/format-fill-color.png");

		// Set common button properties
		control.set({
			toolTipText: "Background Color",
			focusable: false,
			keepFocus: true,
			width: 50,
			height: 16,
			margin: [0, 0]
		});

		// Create the color popup
		var colorPopup = new qx.ui.control.ColorPopup();
		colorPopup.exclude();

		// Arrange to show the color pop-up when the button is pressed
		control.addListener("execute", function (e) {
			colorPopup.placeToWidget(control);
			colorPopup.show();
		}, this.getCkRte());

		colorPopup.addListener("changeValue", function (e) {
			// Retrieve the event data
			var color = e.getData();

			// Call the action function
			this._action(color);
		}, this);

		// Save this control
		this.setControl(control);
	}
});
