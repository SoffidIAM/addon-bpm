<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<mxEditor>
	<ui>
		<resource basename="${c:encodeURL('/addon/bpm/mxgraph/resources/app')}"/>
	</ui>
	<mxDefaultPopupMenu as="popupHandler">
		<add as="cut" action="cut" icon="${c:encodeURL('/addon/bpm/mxgraph/img/cut.svg')}"/>
		<add as="copy" action="copy" icon="${c:encodeURL('/img/copy.svg')}"/>
		<add as="paste" action="paste" icon="${c:encodeURL('/addon/bpm/mxgraph/img/paste.svg')}"/>
		<separator/>
		<add as="delete" action="delete" icon="${c:encodeURL('/img/cancel.svg')}" if="cell"/>
	</mxDefaultPopupMenu>
	<include name="${c:encodeURL('/addon/bpm/mxgraph/config/keyhandler-minimal.xml')}"/>
	<Array as="actions">
		<add as="open"><![CDATA[
			function (editor)
			{
				editor.open(mxUtils.prompt('Enter filename', 'workflow.xml'));
			}
		]]></add>
		<add as="openHref"><![CDATA[
			function (editor, cell)
			{
				cell = cell || editor.graph.getSelectionCell();
				
				if (cell == null)
				{
					cell = editor.graph.getCurrentRoot();

					if (cell == null)
					{
						cell = editor.graph.getModel().getRoot();
					}
				}

				if (cell != null)
				{
					var href = cell.getAttribute('href');
					
					if (href != null && href.length > 0)
					{
						window.open(href);
					}
					else
					{
						mxUtils.alert('No URL defined. Showing properties...');
						editor.execute('showProperties', cell);
					}
				}
			}
		]]></add>
		<add as="editStyle"><![CDATA[
			function (editor)
			{
				var cell = editor.graph.getSelectionCell();
				
				if (cell != null)
				{
					var model = editor.graph.getModel();
					var style = mxUtils.prompt(mxResources.get('enterStyle'), model.getStyle(cell) || '');

					if (style != null)
					{
						model.setStyle(cell, style);
					}
				}
			}
		]]></add>
		<add as="fillColor"><![CDATA[
			function (editor)
			{
				var color = mxUtils.prompt(mxResources.get('enterColorname'), 'red');
				
				if (color != null)
				{
					editor.graph.model.beginUpdate();
					try
					{
						editor.graph.setCellStyles("strokeColor", color);
						editor.graph.setCellStyles("fillColor", color);
					}
					finally
					{
						editor.graph.model.endUpdate();
					}
				}
			}
		]]></add>
		<add as="gradientColor"><![CDATA[
			function (editor)
			{
				var color = mxUtils.prompt(mxResources.get('enterColorname'), 'white');
				
				if (color != null)
				{
					editor.graph.setCellStyles("gradientColor", color);
				}
			}
		]]></add>
		<add as="strokeColor"><![CDATA[
			function (editor)
			{
				var color = mxUtils.prompt(mxResources.get('enterColorname'), 'red');
				
				if (color != null)
				{
					editor.graph.setCellStyles("strokeColor", color);
				}
			}
		]]></add>
		<add as="fontColor"><![CDATA[
			function (editor)
			{
				var color = mxUtils.prompt(mxResources.get('enterColorname'), 'red');
				
				if (color != null)
				{
					editor.graph.setCellStyles("fontColor", color);
				}
			}
		]]></add>
		<add as="fontFamily"><![CDATA[
			function (editor)
			{
				var family = mxUtils.prompt(mxResources.get('enterFontfamily'), 'Arial');
				
				if (family != null && family.length > 0)
				{
					editor.graph.setCellStyles("fontFamily", family);
				}
			}
		]]></add>
		<add as="fontSize"><![CDATA[
			function (editor)
			{
				var size = mxUtils.prompt(mxResources.get('enterFontsize'), '10');
				
				if (size != null && size > 0 && size < 999)
				{
					editor.graph.setCellStyles("fontSize", size);
				}
			}
		]]></add>
		<add as="image"><![CDATA[
			function (editor)
			{
				var image = mxUtils.prompt(mxResources.get('enterImageUrl'),
					'examples/images/image.gif');
				
				if (image != null)
				{
					editor.graph.setCellStyles("image", image);
				}
			}
		]]></add>
		<add as="opacity"><![CDATA[
			function (editor)
			{
				var opacity = mxUtils.prompt(mxResources.get('enterOpacity'), '100');
				
				if (opacity != null && opacity >= 0 && opacity <= 100)
				{
					editor.graph.setCellStyles("opacity", opacity);
				}
			}
		]]></add>
		<add as="straightConnector"><![CDATA[
			function (editor)
			{
				editor.graph.setCellStyle("straightEdge");
			}
		]]></add>
		<add as="elbowConnector"><![CDATA[
			function (editor)
			{
				editor.graph.setCellStyle("");
			}
		]]></add>
		<add as="arrowConnector"><![CDATA[
			function (editor)
			{
				editor.graph.setCellStyle("arrowEdge");
			}
		]]></add>
		<add as="toggleOrientation"><![CDATA[
			function (editor, cell)
			{
				editor.graph.toggleCellStyles(mxConstants.STYLE_HORIZONTAL, true);
			}
		]]></add>
		<add as="toggleRounded"><![CDATA[
			function (editor)
			{
				editor.graph.toggleCellStyles(mxConstants.STYLE_ROUNDED);
			}
		]]></add>
		<add as="toggleShadow"><![CDATA[
			function (editor)
			{
				editor.graph.toggleCellStyles(mxConstants.STYLE_SHADOW);
			}
		]]></add>
		<add as="horizontalTree"><![CDATA[
			function (editor, cell)
			{
				cell = cell || editor.graph.getSelectionCell();
				
				if (cell == null)
				{
					cell = editor.graph.getDefaultParent();
				}
				
				editor.treeLayout(cell, true);
			}
		]]></add>
		<add as="verticalTree"><![CDATA[
			function (editor, cell)
			{
				cell = cell || editor.graph.getSelectionCell();
				
				if (cell == null)
				{
					cell = editor.graph.getDefaultParent();
				}
				
				editor.treeLayout(cell, false);
			}
		]]></add>
	</Array>
</mxEditor>
