<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<c:set var="self" value="${requestScope.arg.self}"/>
<c:set var="edid" value="${self.uuid}!ed"/>
<div id="${self.uuid}"${self.outerAttrs} z.type="mxgraph.mxgraph.MxGraph"
${c:attr('class',self.sclass)}${c:attr('style',self.style)} ${self.innerAttrs}>
<div id="${self.uuid}!toolbar" class="${self.sclass}_toolbar"></div>
<div id="${self.uuid}!graph"  class="${self.sclass}_graph"></div>
<div id="${self.uuid}!status" class="${self.sclass}_status"></div>
</div>

