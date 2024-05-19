<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<mxEditor defaultGroup="group" defaultEdge="edge"
	layoutDiagram="1" maintainSwimlanes="1"
	swimlaneRequired="1" forcedInserting="1"
	helpWindowImage="${c:encodeURL('/addon/bpm/mxgraph/images/help.gif')}"
	tasksWindowImage="${c:encodeURL('/addon/bpm/mxgraph/images/tasks.gif')}">
	<include name="config/editor-commons.xml"/>
	<add as="onInit"><![CDATA[
		function ()
		{
			// Disables removing cells from parents
			this.graph.graphHandler.setRemoveCellsFromParent(false);
			this.showTasks();
			this.showHelp();
		}
	]]></add>
	<ui>
		<stylesheet name="css/process.css"/>
		<add as="graph" element="graph"/>
		<add as="status" element="status"/>
		<add as="toolbar" element="toolbar"/>
	</ui>
	<Array as="cycleAttributeValues">
		<add value="#83027F"/>
		<add value="#66B922"/>
		<add value="#808913"/>
		<add value="#CF0056"/>
		<add value="#4679B6"/>
	</Array>
	<Array as="templates">
		<add as="edge">
			<Edge label="" description="">
				<mxCell edge="1">
					<mxGeometry as="geometry" isRelative="1"/>
				</mxCell>
			</Edge>
		</add>
		<add as="task">
			<Task label="Task">
				<mxCell vertex="1" style="rounded">	
					<mxGeometry as="geometry" width="80" height="30"/>
				</mxCell>
			</Task>
		</add>
		<add as="hline">
			<Shape label="">
				<mxCell vertex="1" style="ellipse">		
					<mxGeometry as="geometry" width="60" height="10"/>
				</mxCell>
			</Shape>
		</add>
	</Array>
	<add as="createTasks"><![CDATA[
		function (div)
		{
			var off = 30;
			
			if (this.graph != null)
			{
				var layer = this.graph.getModel().getRoot().getChildAt(0);
				
				if (layer == null || layer.getChildCount() == 0)
				{
					mxUtils.para(div, 'Examples:');
					mxUtils.linkInvoke(div, 'Withdrawal', this, 'open',
						'diagrams/withdrawal.xml', off);
					mxUtils.br(div);
				}
				else
				{
					mxUtils.para(div, 'Clipboard:');
					
					if (!this.graph.isSelectionEmpty())
					{
						mxUtils.linkAction(div, 'Copy to Clipboard', this, 'copy', off);
						mxUtils.br(div);
					}
					
					mxUtils.linkAction(div, 'Paste from Clipboard', this, 'paste', off);
					mxUtils.br(div);
					
					if (!this.graph.isSelectionEmpty())
					{
						mxUtils.linkAction(div, 'Delete Selected Cells', this, 'delete', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, 'Clear Selection', this, 'selectNone', off);
						mxUtils.br(div);
					}
					else
					{
						mxUtils.linkAction(div, 'Select All Cells', this, 'selectAll', off);
						mxUtils.br(div);
					}
					
					mxUtils.para(div, 'History:');
					mxUtils.linkAction(div, 'Undo Last Change', this, 'undo', off);
					mxUtils.br(div);
					mxUtils.linkAction(div, 'Redo Last Change', this, 'redo', off);
					mxUtils.br(div);
				}
				
				mxUtils.br(div);
			}
		}
	]]></add>
	<mxGraph as="graph" alternateEdgeStyle="verticalEdge"
		swimlaneNesting="0" dropEnabled="1">
		<add as="isAutoSizeCell"><![CDATA[
			function(cell)
			{
				return this.isSwimlane(cell) || this.isTask(cell);
			}
		]]></add>
		<add as="isValidRoot"><![CDATA[
			function(cell)
			{
				return !this.isSwimlane(cell);
			}
		]]></add>
		<add as="isCellFoldable"><![CDATA[
			function(cell, collapse)
			{
				return !this.isSwimlane(cell) &&
					cell.getChildCount() > 0;
			}
		]]></add>
		<add as="isSwimlane">
			function (cell)
			{
				return mxUtils.isNode(this.model.getValue(cell), 'swimlane');
			}
		</add>
		<add as="isTask">
			function (cell)
			{
				return mxUtils.isNode(this.model.getValue(cell), 'task');
			}
		</add>
		<add as="isNode">
			function (cell)
			{
				return mxUtils.isNode(this.model.getValue(cell), 'node');
			}
		</add>
		<add as="isAllowOverlapParent">
			function(cell)
			{
				return false;
			}
		</add>
		<add as="getTooltipForCell"><![CDATA[
			function(cell)
			{
				return '<b>'+cell.getAttribute('label')+
						'</b> ('+cell.getId()+')'+
						'<br>Style: '+cell.getStyle()+
						'<br>Edges: '+cell.getEdgeCount()+
						'<br>Children: '+cell.getChildCount();
			}
		]]></add>
		<add as="convertValueToString"><![CDATA[
			function(cell)
			{
				return cell.getAttribute('label');
			}
		]]></add>
		<mxStylesheet as="stylesheet">
			<add as="defaultVertex">
				<add as="shape" value="rectangle"/>
				<add as="perimeter" value="rectanglePerimeter"/>
				<add as="fontColor" value="black"/>
				<add as="fontSize" value="10"/>
				<add as="align" value="center"/>
				<add as="verticalAlign" value="middle"/>
				<add as="fillColor" value="indicated"/>
				<add as="indicatorColor" value="swimlane"/>
				<add as="gradientColor" value="white"/>
			</add>
			<add as="group">
				<add as="shape" value="rectangle"/>
				<add as="perimeter" value="rectanglePerimeter"/>
				<add as="fontSize" value="10"/>
				<add as="align" value="center"/>
				<add as="verticalAlign" value="middle"/>
				<add as="strokeColor" value="gray"/>
				<add as="dashed" value="1"/>
			</add>
			<add as="defaultEdge">
				<add as="shape" value="connector"/>
				<add as="fontSize" value="10"/>
				<add as="rounded" value="1"/>
				<add as="strokeColor" value="gray"/>
				<add as="edgeStyle" value="elbowEdgeStyle"/>
				<add as="endArrow" value="classic"/>
			</add>
			<add as="verticalEdge">
				<add as="elbow" value="vertical"/>
			</add>
			<add as="swimlane">
				<add as="shape" value="swimlane"/>
				<add as="perimeter" value="rectanglePerimeter"/>
				<add as="fontSize" value="12"/>
				<add as="startSize" value="36"/>
				<add as="rounded" value="1"/>
				<add as="align" value="center"/>
				<add as="verticalAlign" value="top"/>
				<add as="spacingTop" value="8"/>
				<add as="fontColor" value="white"/>
				<add as="separatorColor" value="#c0c0c0"/>
			</add>
			<add as="rounded">
				<add as="rounded" value="1"/>
			</add>
			<add as="ellipse">
				<add as="shape" value="label"/>
				<add as="indicatorShape" value="ellipse"/>
				<add as="indicatorWidth" value="34"/>
				<add as="indicatorHeight" value="34"/>
				<add as="imageVerticalAlign" value="top"/>
				<add as="imageAlign" value="center"/>
				<add as="spacingTop" value="40"/>
				<add as="perimeter" value="rectanglePerimeter"/>
				<add as="fontSize" value="10"/>
				<add as="align" value="center"/>
				<add as="verticalAlign" value="top"/>
				<add as="indicatorColor" value="swimlane"/>
				<add as="indicatorGradientColor" value="white"/>
				<add as="fillColor" value="none"/>
				<add as="gradientColor" value="none"/>
			</add>
			<add as="rhombus" extend="ellipse">
				<add as="indicatorShape" value="rhombus"/>
			</add>
			<add as="actor" extend="ellipse">
				<add as="indicatorShape" value="actor"/>
				<add as="indicatorWidth" value="26"/>
			</add>
			<add as="cylinder" extend="actor">
				<add as="indicatorShape" value="cylinder"/>
				<add as="imageVerticalAlign" value="bottom"/>
				<add as="indicatorHeight" value="30"/>
				<add as="verticalAlign" value="top"/>
				<add as="spacingTop" value="0"/>
			</add>
			<add as="hline">
				<add as="shape" value="line"/>
				<add as="strokeWidth" value="3"/>
				<add as="perimeter" value="rectanglePerimeter"/>
				<add as="fontColor" value="black"/>
				<add as="fontSize" value="10"/>
				<add as="align" value="center"/>
				<add as="verticalAlign" value="bottom"/>
				<add as="strokeColor" value="indicated"/>
			</add>
		</mxStylesheet>
		<mxGraphModel as="model">
			<add as="valueForCellChanged"><![CDATA[
				function(cell, value)
				{
					var previous = null;
					
					if (isNaN(value.nodeType))
					{
						previous = cell.getAttribute('label');
						cell.setAttribute('label', value);
					}
					else
					{
						previous = cell.value;
						cell.value = value;
					}
					
					return previous;
				}
			]]></add>
			<root>
				<Workflow label="MyWorkflow" id="0"/>
				<Layer label="Default Layer">
					<mxCell parent="0"/>
				</Layer>
			</root>
		</mxGraphModel>
	</mxGraph>
	<mxDefaultToolbar as="toolbar">
		<add as="Save" action="save" icon="${c:encodeURL('/addon/bpm/mxgraph/images/save.gif')}"/>
		<separator/>
		<add as="Undo" action="undo" icon="${c:encodeURL('/addon/bpm/mxgraph/images/undo.gif')}"/>
		<add as="Redo" action="redo" icon="${c:encodeURL('/addon/bpm/mxgraph/images/redo.gif')}"/>
		<add as="Cut" action="cut" icon="${c:encodeURL('/addon/bpm/mxgraph/images/cut.gif')}"/>
		<add as="Copy" action="copy" icon="${c:encodeURL('/addon/bpm/mxgraph/images/copy.gif')}"/>
		<add as="Paste" action="paste" icon="${c:encodeURL('/addon/bpm/mxgraph/images/paste.gif')}"/>
		<add as="Delete" action="delete" icon="${c:encodeURL('/addon/bpm/mxgraph/images/delete.gif')}"/>
		<add as="Group" action="group" icon="${c:encodeURL('/addon/bpm/mxgraph/images/group.gif')}"/>
		<add as="Ungroup" action="ungroup" icon="${c:encodeURL('/addon/bpm/mxgraph/images/ungroup.gif')}"/>
		<separator/>
		<add as="Select" mode="select" icon="${c:encodeURL('/addon/bpm/mxgraph/images/select.gif')}"/>
		<add as="Pan" mode="pan" icon="${c:encodeURL('/addon/bpm/mxgraph/images/pan.gif')}"/>
		<add as="Connect" mode="connect" icon="${c:encodeURL('/addon/bpm/mxgraph/images/connect.gif')}"/>
		<separator/>
		<add as="Swimlane" template="swimlane" icon="${c:encodeURL('/addon/bpm/mxgraph/images/swimlane.gif')}"/>
		<add as="Task" template="task" icon="${c:encodeURL('/addon/bpm/mxgraph/images/rectangle.gif')}"/>
		<add as="Subprocess" template="subprocess" icon="${c:encodeURL('/addon/bpm/mxgraph/images/rounded.gif')}"/>
		<add as="Ellipse" template="shape" style="ellipse" icon="${c:encodeURL('/addon/bpm/mxgraph/images/ellipse.gif')}"/>
		<add as="Rhombus" template="shape" style="rhombus" icon="${c:encodeURL('/addon/bpm/mxgraph/images/rhombus.gif')}"/>
		<add as="Actor" template="shape" style="actor" icon="${c:encodeURL('/addon/bpm/mxgraph/images/actor.gif')}"/>
		<add as="Cylinder" template="shape" style="cylinder" icon="${c:encodeURL('/addon/bpm/mxgraph/images/cylinder.gif')}"/>
		<add as="Line" template="hline" style="hline" icon="${c:encodeURL('/addon/bpm/mxgraph/images/hline.gif')}"/>
		<separator/>
		<add as="Fit" action="fit" icon="${c:encodeURL('/addon/bpm/mxgraph/images/zoom.gif')}"/>
		<add as="Zoom In" action="zoomIn" icon="${c:encodeURL('/addon/bpm/mxgraph/images/zoomin.gif')}"/>
		<add as="Zoom Out" action="zoomOut" icon="${c:encodeURL('/addon/bpm/mxgraph/images/zoomout.gif')}"/>
		<add as="Actual Size" action="actualSize" icon="${c:encodeURL('/addon/bpm/mxgraph/images/zoomactual.gif')}"/>
		<add as="Zoom" action="zoom" icon="${c:encodeURL('/addon/bpm/mxgraph/images/zoom.gif')}"/>
		<separator/>
		<add as="outline" action="toggleOutline" icon="${c:encodeURL('/addon/bpm/mxgraph/images/outline.gif')}"/>
		<add as="Tasks" action="toggleTasks" icon="${c:encodeURL('/addon/bpm/mxgraph/images/tasks.gif')}"/>
		<add as="Help" action="toggleHelp" icon="${c:encodeURL('/addon/bpm/mxgraph/images/help.gif')}"/>
		<add as="Console" action="toggleConsole" icon="${c:encodeURL('/addon/bpm/mxgraph/images/console.gif')}"/>
	</mxDefaultToolbar>
</mxEditor>
