/** 
 * ZK port of Codemirror - Real Time Syntax Highlighting Editor written in JavaScript - http://codemirror.net/
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the 
 * GNU Lesser General Public License as published by the Free Software Foundation.
 * 
 * Read the full licence: http://www.opensource.org/licenses/lgpl-license.php
 */

zkMxGraph = {};

zkMxGraph.createEditor = function(ed) {
	var editor = null;
	
	try
	{
		if (!mxClient.isBrowserSupported())
		{
			mxUtils.error('Browser is not supported!', 200, false);
		}
		else
		{
			var cfg = mxUtils.load(ed.getAttribute("z.config")).getDocumentElement();
			var ui = cfg.getElementsByTagName("ui").item(0);
			for (var ch = ui.firstElementChild; ch != null; ch = ch.nextElementSibling) {
				if (ch.getAttribute("as") == "graph")
					ch.setAttribute("element", ed.id+"!graph" );
				if (ch.getAttribute("as") == "status")
					ch.setAttribute("element", ed.id+"!status" );
				if (ch.getAttribute("as") == "toolbar")
					ch.setAttribute("element", ed.id+"!toolbar" );
			}
			mxObjectCodec.allowEval = true;
			ed.editor = editor = new mxEditor(cfg, ed);
			mxObjectCodec.allowEval = false;
			
			// Adds active border for panning inside the container
			editor.graph.createPanningManager = function()
			{
				var pm = new mxPanningManager(this);
				pm.border = 30;
				
				return pm;
			};
			
			editor.graph.allowAutoPanning = true;
			editor.graph.timerAutoScroll = true;
			
			// Updates the window title after opening new files
			var title = ed.getAttribute("title");
			
			editor.graph.getModel().addListener(mxEvent.CHANGE, (e)=> {
				var enc = new mxCodec();
				var data = enc.encode(editor.graph.getModel());
				var xml = mxUtils.getXml(data);
				var req = {uuid: ed.id, cmd: "onChange", data : [xml], ignorable: true};
				zkau.send (req, 5);
			});

			editor.graph.addListener(mxEvent.CLICK, (e, t)=> {
				var cell = t.properties.cell;
				if (cell) {
					var req = {uuid: ed.id, cmd: "onSelect", data : [cell.id], ignorable: true};
					zkau.send (req, 5);
					ed.lastCell = cell;
				} else if (!editor.graph.isCellSelected(ed.lastCell)) {
					var req = {uuid: ed.id, cmd: "onSelect", data : [], ignorable: true};
					zkau.send (req, 5);
				}
			});


			// Displays version in statusbar
			editor.setStatus('Soffid BPM Editor ');

		}
	}
	catch (e)
	{
		// Shows an error message if the editor cannot start
		mxUtils.alert('Cannot start application: ' + e.message);
		throw e; // for debugging
	}

	return editor;
}

zkMxGraph.updateLabel = function(ed, id, label) {
	var model = ed.editor.graph.getModel();
	for (var i in model.cells) {
		var cell = model.cells[i];
		if (cell.id == id)
			model.setValue(cell, label);
	}	
}


zkMxGraph.updateImg = function(ed) {
	var graph = document.getElementById(ed.id+'!graph');
	var svg = document.getElementById(ed.id+'!graph').firstElementChild;
	var img = document.getElementById(ed.id+"!img");
	var canvas = document.getElementById(ed.id+'!canvas');
	
	// get svg data
	var xml = new XMLSerializer().serializeToString(svg);
	
	// make it base64
	var svg64 = btoa(xml);
	var image64 = 'data:image/svg+xml;base64,' + svg64;
	
	// set it as the source of the img element
	img.onload = function() {
	    // draw the image onto the canvas
	    var canvas = document.createElement('canvas');
		var cr = svg.getClientRects()[0];
	    canvas.width = cr.width;
	    canvas.height = cr.height;
	    canvas.getContext('2d').drawImage(img, 0, 0);
	    var v = canvas.toDataURL();
		var req = {uuid: ed.id, cmd: "onImage", data : [v], ignorable: false};
		zkau.send (req, 5);
	}
	img.src = image64;	
}

zkMxGraph.init = function (ed) {
		mxGraph.prototype.htmlLabels = true;
	
		mxGraph.prototype.isWrapping = function(cell)
		{
			return true;
		};
		
		mxConstants.DEFAULT_HOTSPOT = 1;
		
		// Enables guides
		mxGraphHandler.prototype.guidesEnabled = true;
		
	    // Alt disables guides
	    mxGuide.prototype.isEnabledForEvent = function(evt)
	    {
	    	return !mxEvent.isAltDown(evt);
	    };
		
		// Enables snapping waypoints to terminals
		mxEdgeHandler.prototype.snapToTerminals = true;
		
		zkMxGraph.createEditor(ed);
		
		if (ed.getAttribute("z.model")) {
			zkMxGraph.loadModel(ed, ed.getAttribute("z.model"));
		}
			
};

zkMxGraph.loadModel=function(ed, model) {
	var doc = mxUtils.parseXml(model);
	codec = new mxCodec(doc);
	codec.decode(doc.documentElement, ed.editor.graph.getModel());
}

zkMxGraph.refresh=function(ed) {
}

zkMxGraph.onSize=function(ed) {
}

zkMxGraph.cleanup = function (ed) {
};

/** Called by the server to set the attribute. */
zkMxGraph.setAttr = function (ed, name, value) {
	switch (name) {
	case "z.model":
			zkMxGraph.loadModel(ed, value);
			break;
	case "value":
	}
	return false;
};

