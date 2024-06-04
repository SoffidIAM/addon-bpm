<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/web/bpm.dsp.tld" prefix="s" %>
<mxGraph alternateEdgeStyle="verticalEdge" dropEnabled="1" foldingEnabled="0">
	<add as="isAutoSizeCell"><![CDATA[
		function(cell)
		{
			return false;
		}
	]]></add>
	<add as="isSwimlane"><![CDATA[
		function (cell)
		{
			return mxUtils.isNode(this.model.getValue(cell), 'task') || 
				mxUtils.isNode(this.model.getValue(cell), 'node') ;
		}
	]]></add>
	<add as="isAllowOverlapParent"><![CDATA[
		function(cell)
		{
			return false;
		}
	]]></add>
	<add as="isValidDropTarget"><![CDATA[
		function(cell, cells, evt)
		{
			if (cells) {
				for (var i = 0; i < cells.length; i++) {
					var cell2 = cells[i];
					if (cell2 != cell && ! mxUtils.isNode(this.model.getValue(cell2), 'timer')) {
						throw "Error";
					}
				}
			}
			if (mxUtils.isNode(this.model.getValue(cell), 'task') ) {
				return true;
			} else {
				throw "Error";
			}
		}
	]]></add>
	<add as="getTooltipForCell"><![CDATA[
		function(cell)
		{
			var href = cell.getAttribute('href');
			href = (href != null && href.length > 0) ?
				'<br>'+href : '';
			var maxlen = 30;
			var desc = cell.getAttribute('description');
			if (desc == null || desc.length == 0)
			{
				desc = '';
			}
			else if (desc.length < maxlen)
			{
				desc = '<br>'+desc;
			}
			else
			{
				desc = '<br>'+desc.substring(0, maxlen)+'...';
			}
			return '<b>'+cell.getAttribute('label')+
					'</b> ('+cell.getId()+')'+href+desc+
					'<br>Edges: '+cell.getEdgeCount()+
					'<br>Children: '+cell.getChildCount();
		}
	]]></add>
	<add as="convertValueToString">
		function(cell)
		{
			return cell.getAttribute('label');
		}
	</add>
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
		<add as="valueForCellChanged2"><![CDATA[
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
			<Workflow label="MyWorkflow" description="" href="" id="0"/>
			<Layer label="Default Layer">
				<mxCell parent="0"/>
			</Layer>
		</root>
	</mxGraphModel>
	<mxStylesheet as="stylesheet">
		<add as="defaultVertex">
			<add as="shape" value="label"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="middle"/>
			<add as="strokeColor" value="black"/>
		</add>
		<add as="defaultEdge">
			<add as="shape" value="connector"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="rounded" value="1"/>
			<add as="edgeStyle" value="elbowEdgeStyle"/>
			<add as="endArrow" value="classic"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="middle"/>
			<add as="strokeColor" value="#ff4080"/>
			<add as="strokeWidth" value="3"/>
		</add>
		<add as="verticalEdge">
			<add as="elbow" value="vertical"/>
		</add>
		<add as="straightEdge">
			<add as="shape" value="connector"/>
			<add as="rounded" value="0"/>
			<add as="edgeStyle" value=""/>
			<add as="endArrow" value="classic"/>
			<add as="strokeColor" value="#ff4080"/>
			<add as="strokeWidth" value="3"/>
		</add>
		<add as="arrowEdge">
			<add as="shape" value="arrow"/>
			<add as="fillColor" value="red"/>
		</add>
		<add as="swimlane">
			<add as="shape" value="swimlane"/>
			<add as="fontSize" value="12"/>
			<add as="startSize" value="23"/>
			<add as="horizontal" value="0"/>
			<add as="verticalAlign" value="top"/>
			<add as="fontColor" value="white"/>
			<add as="labelBackgroundColor" value="none"/>
		</add>
		<add as="group">
			<add as="shape" value="rectangle"/>
			<add as="rounded" value="1"/>
			<add as="verticalAlign" value="top"/>
			<add as="strokeColor" value="black"/>
			<add as="dashed" value="1"/>
			<add as="opacity" value="50"/>
		</add>
		<add as="task">
			<add as="rounded" value="1"/>
			<add as="opacity" value="100"/>
			<add as="fillColor" value="#b3f7ff"/>
			<add as="labelBackgroundColor" value="#b3f7ff"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/task.svg')}"/>
			<add as="strokeWidth" value="3"/>
			<add as="strokeColor" value="#404040"/>
		</add>
		<add as="grant">
			<add as="rounded" value="1"/>
			<add as="opacity" value="100"/>
			<add as="fillColor" value="#b3f7ff"/>
			<add as="labelBackgroundColor" value="#b3f7ff"/>
			<add as="image" value="${s:includeImage('/img/ok.svg')}"/>
			<add as="strokeWidth" value="3"/>
			<add as="strokeColor" value="#404040"/>
		</add>
		<add as="match">
			<add as="rounded" value="1"/>
			<add as="opacity" value="100"/>
			<add as="fillColor" value="#b3f7ff"/>
			<add as="labelBackgroundColor" value="#b3f7ff"/>
			<add as="image" value="${s:includeImage('/img/search.svg')}"/>
			<add as="strokeWidth" value="3"/>
			<add as="strokeColor" value="#404040"/>
		</add>
		<add as="node">
			<add as="rounded" value="1"/>
			<add as="opacity" value="100"/>
			<add as="fontColor" value="#000000"/>
			<add as="fillColor" value="#b7cce1"/>
			<add as="labelBackgroundColor" value="#b7cce1"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/node-white.svg')}"/>
			<add as="strokeWidth" value="3"/>
			<add as="strokeColor" value="#404040"/>
		</add>
		<add as="apply">
			<add as="rounded" value="1"/>
			<add as="opacity" value="100"/>
			<add as="fontColor" value="#000000"/>
			<add as="fillColor" value="#b7cce1"/>
			<add as="labelBackgroundColor" value="#b7cce1"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/apply-white.svg')}"/>
			<add as="strokeWidth" value="3"/>
			<add as="strokeColor" value="#404040"/>
		</add>
		<add as="ellipse">
			<add as="shape" value="ellipse"/>
			<add as="perimeter" value="ellipsePerimeter"/>
		</add>
		<add as="rhombus">
			<add as="shape" value="rhombus"/>
			<add as="perimeter" value="rhombusPerimeter"/>
		</add>
		<add as="actor">
			<add as="shape" value="actor"/>
		</add>
		<add as="start">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/start.svg')}"/>
		</add>
		<add as="end">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/end.svg')}"/>
		</add>
		<add as="fork">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/split.svg')}"/>
		</add>
		<add as="join">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/join.svg')}"/>
		</add>
		<add as="decision">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/decision.svg')}"/>
		</add>
		<add as="system">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/system.svg')}"/>
		</add>
		<add as="timer">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/timer-black.svg')}"/>
		</add>
		<add as="mail">
			<add as="shape" value="image"/>
			<add as="perimeter" value="rectanglePerimeter"/>
			<add as="labelBackgroundColor" value="white"/>
			<add as="fontSize" value="10"/>
			<add as="align" value="center"/>
			<add as="verticalAlign" value="top"/>
			<add as="verticalLabelPosition" value="bottom"/>
			<add as="image" value="${s:includeImage('/addon/bpm/mxgraph/img/mail.svg')}"/>
		</add>
	</mxStylesheet>
</mxGraph>
