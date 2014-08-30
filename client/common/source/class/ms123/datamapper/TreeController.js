/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
qx.Class.define("ms123.datamapper.TreeController", {
	extend: qx.data.controller.Tree,


	construct: function (model, target, childPath, labelPath) {
		this.base(arguments,model,target,childPath,labelPath);
		this._eventsEnabled=true;
	},



	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {},



	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {

		/**
		 ---------------------------------------------------------------------------
		 EVENT HANDLER
		 ---------------------------------------------------------------------------
		 */
		__changeModelChildren: function (ev) {
			if( this._eventsEnabled === true ){
				var children = ev.getTarget();
				var treeNode = this.__childrenRef[children.toHashCode()].treeNode;
				var modelNode = this.__childrenRef[children.toHashCode()].modelNode;
				this.__updateTreeChildren(treeNode, modelNode);

				this._updateSelection();
			}
		},
		enableChangeEvents:function(enable){
			this._eventsEnabled=enable;
		}
	},



	/**
	 *****************************************************************************
	 DESTRUCTOR
	 *****************************************************************************
	 */

	destruct: function () {}
});
