<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<mxDefaultToolbar>
	<add as="select" mode="select" icon="${c:encodeURL('/addon/bpm/mxgraph/img/pointer.svg')}"/>
	<add as="pan" mode="pan" icon="${c:encodeURL('/addon/bpm/mxgraph/img/hand.svg')}"/>
	<add as="connect" mode="connect" icon="${c:encodeURL('/addon/bpm/mxgraph/img/conn-1.svg')}"><![CDATA[
		function (editor)
		{
			if (editor.defaultEdge != null)
			{
				editor.defaultEdge.style = 'straightEdge';
			}
		} 
	]]></add>
	<add as="connect" mode="connect" icon="${c:encodeURL('/addon/bpm/mxgraph/img/conn-2.svg')}"><![CDATA[
		function (editor)
		{
			if (editor.defaultEdge != null)
			{
				editor.defaultEdge.style = null;
			}
		}
	]]></add>
	<add as="undo" action="undo" icon="${c:encodeURL('/img/undo.svg')}"/>
	<add as="redo" action="redo" icon="${c:encodeURL('/img/next.svg')}"/>
	<add as="cut" action="cut" icon="${c:encodeURL('/addon/bpm/mxgraph/img/cut.svg')}"/>
	<add as="copy" action="copy" icon="${c:encodeURL('/img/copy.svg')}"/>
	<add as="paste" action="paste" icon="${c:encodeURL('/addon/bpm/mxgraph/img/paste.svg')}"/>
	<add as="delete" action="delete" icon="${c:encodeURL('/img/cancel.svg')}"/>
	<add as="Start state" template="start" icon="${c:encodeURL('/addon/bpm/mxgraph/img/start.svg')}"/>
	<add as="End state" template="end" icon="${c:encodeURL('/addon/bpm/mxgraph/img/end.svg')}"/>
	<add as="Fork" template="fork" icon="${c:encodeURL('/addon/bpm/mxgraph/img/split.svg')}"/>
	<add as="Join" template="join" icon="${c:encodeURL('/addon/bpm/mxgraph/img/join.svg')}"/>
	<add as="Decision" template="decision" icon="${c:encodeURL('/addon/bpm/mxgraph/img/decision.svg')}"/>
	<add as="System" template="system" icon="${c:encodeURL('/addon/bpm/mxgraph/img/system.svg')}"/>
	<add as="Node" template="node" icon="${c:encodeURL('/addon/bpm/mxgraph/img/node.svg')}"/>
	<add as="Task" template="task" icon="${c:encodeURL('/addon/bpm/mxgraph/img/task.svg')}"/>
	<add as="Mail" template="mail" icon="${c:encodeURL('/addon/bpm/mxgraph/img/mail.svg')}"/>
	<add as="Timer" template="timer" icon="${c:encodeURL('/addon/bpm/mxgraph/img/timer.svg')}"/>
	<add as="zoomOut" action="zoomOut" icon="${c:encodeURL('/addon/bpm/mxgraph/img/zoomout.svg')}"/>
	<add as="zoomIn" action="zoomIn" icon="${c:encodeURL('/addon/bpm/mxgraph/img/zoomin.svg')}"/>
	<add as="fit" action="fit" icon="${c:encodeURL('/addon/bpm/mxgraph/img/fit.svg')}"/>
	<add as="actualSize" action="actualSize" icon="${c:encodeURL('/addon/bpm/mxgraph/img/nozoom.svg')}"/>
</mxDefaultToolbar>
