<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/web/bpm.dsp.tld" prefix="s" %>
<mxEditor defaultGroup="group" defaultEdge="edge"
	helpWindowImage="${c:encodeURL('/addon/bpm/mxgraph/images/help.gif')}"
	tasksWindowImage="${c:encodeURL('/addon/bpm/mxgraph/images/tasks.gif')}"
	forcedInserting="0"
	swimlaneRequired="0">
	<include name="${c:encodeURL('/addon/bpm/mxgraph/config/editor-commons.xml.dsp')}"/>
	<add as="onInit">
		function ()
		{
//			this.showTasks();
		}
	</add>
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
					<mxGeometry as="geometry" relative="1"/>
				</mxCell>
			</Edge>
		</add>
		<add as="task">
			<Task label="Task" description="" href="" type="task">
				<mxCell vertex="1" style="task">	
					<mxGeometry as="geometry" width="128" height="64"/>
				</mxCell>
			</Task>
		</add>
		<add as="start">
			<Symbol label="Start" description="" href="" type="start">
				<mxCell vertex="1" style="start">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="end">
			<Symbol label="End" description="" href="" type="end">
				<mxCell vertex="1" style="end">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="fork">
			<Symbol label="Fork" description="" href="" type="fork">
				<mxCell vertex="1" style="split">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="join">
			<Symbol label="Join" description="" href="" type="join">
				<mxCell vertex="1" style="join">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="decision">
			<Symbol label="Decision" description="" href="" type="decision">
				<mxCell vertex="1" style="decision">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="node">
			<Node label="Process node" description="" href="" type="apply">
				<mxCell vertex="1" style="apply">	
					<mxGeometry as="geometry" width="128" height="64"/>
				</mxCell>
			</Node>
		</add>
		<add as="system">
			<Symbol label="System" description="" href="" type="system">
				<mxCell vertex="1" style="system">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
		<add as="timer">
			<Timer description="" href="" type="timer">
				<mxCell vertex="1" style="timer">		
					<mxGeometry as="geometry" width="24" height="24"/>
				</mxCell>
			</Timer>
		</add>
		<add as="mail">
			<Symbol label="Mail" description="" href="" type="mail">
				<mxCell vertex="1" style="mail">		
					<mxGeometry as="geometry" width="64" height="64"/>
				</mxCell>
			</Symbol>
		</add>
	</Array>
	<!--
	
	<add as="createTasks"><![CDATA[
		function (div)
		{
			var off = 30;
			
			if (this.graph != null)
			{
				var layer = this.graph.model.root.getChildAt(0);
				mxUtils.para(div,  mxResources.get('examples'));
				mxUtils.linkInvoke(div, mxResources.get('newDiagram'), this,
					'open', 'diagrams/empty.xml', off);
				mxUtils.br(div);
				mxUtils.linkInvoke(div, mxResources.get('swimlanes'), this,
					'open', 'diagrams/swimlanes.xml', off);
				mxUtils.br(div);
				mxUtils.linkInvoke(div, mxResources.get('travelBooking'), this,
					'open', 'diagrams/travel-booking.xml', off);
				mxUtils.br(div);
				
				if (!this.graph.isSelectionEmpty())
				{
					var cell = this.graph.getSelectionCell();
					if (this.graph.getSelectionCount() == 1 &&
						(this.graph.model.isVertex(cell) &&
						cell.getEdgeCount() > 0) || this.graph.isSwimlane(cell))
					{
						mxUtils.para(div, mxResources.get('layout'));
						mxUtils.linkAction(div, mxResources.get('verticalTree'),
							this, 'verticalTree', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('horizontalTree'),
							this, 'horizontalTree', off);
						mxUtils.br(div);
					}
					
					mxUtils.para(div, mxResources.get('format'));
					
					if (mxUtils.isNode(cell.value, 'Symbol'))
					{
						mxUtils.linkAction(div, mxResources.get('image'),
							this, 'image', off);
						mxUtils.br(div);
					}
					else
					{
						mxUtils.linkAction(div, mxResources.get('opacity'),
							this, 'opacity', off);
						mxUtils.br(div);
						if (this.graph.model.isVertex(cell) ||
							(cell.style != null && 
							cell.style.indexOf("arrowEdge") >= 0))
						{
							mxUtils.linkAction(div, mxResources.get('gradientColor'),
								this, 'gradientColor', off);
							mxUtils.br(div);
						}
						if (this.graph.model.isEdge(cell))
						{
							mxUtils.linkAction(div, 'Straight Connector', this, 'straightConnector', off);
							mxUtils.br(div);
							mxUtils.linkAction(div, 'Elbow Connector', this, 'elbowConnector', off);
							mxUtils.br(div);
							mxUtils.linkAction(div, 'Arrow Connector', this, 'arrowConnector', off);
							mxUtils.br(div);
						}
					}
					
					mxUtils.linkAction(div, mxResources.get('toggleRounded'), this, 'toggleRounded', off);
					mxUtils.br(div);
					if (this.graph.isSwimlane(cell) || this.graph.model.isEdge(cell))
					{
						mxUtils.linkAction(div, mxResources.get('toggleOrientation'), this, 'toggleOrientation', off);
						mxUtils.br(div);
					}
					
					if (this.graph.getSelectionCount() > 1)
					{
						mxUtils.para(div, mxResources.get('align'));
						mxUtils.linkAction(div, mxResources.get('left'),
							this, 'alignCellsLeft', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('center'),
							this, 'alignCellsCenter', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('right'),
							this, 'alignCellsRight', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('top'),
							this, 'alignCellsTop', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('middle'),
							this, 'alignCellsMiddle', off);
						mxUtils.br(div);
						mxUtils.linkAction(div, mxResources.get('bottom'),
							this, 'alignCellsBottom', off);
						mxUtils.br(div);
					}
					
					mxUtils.para(div, mxResources.get('selection'));
					mxUtils.linkAction(div, mxResources.get('clearSelection'),
						this, 'selectNone', off);
					mxUtils.br(div);
				}
				else if (layer.getChildCount() > 0)
				{
					mxUtils.para(div, mxResources.get('selection'));
					mxUtils.linkAction(div, mxResources.get('selectAll'),
						this, 'selectAll', off);
					mxUtils.br(div);
				}
				
				mxUtils.br(div);
			}
		}
	]]></add>
	-->
</mxEditor>
