package com.soffid.iam.addons.bpm.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.api.MetadataScope;

public class AttributeEntityDaoImpl extends AttributeEntityDaoBase {

	public void toAttribute(AttributeEntity sourceEntity, Attribute targetVO) {
		super.toAttribute(sourceEntity, targetVO);
		if (sourceEntity.getValues() == null || sourceEntity.getValues().length() == 0)
			targetVO.setValues ( new LinkedList<String>() );
		else
		{
			List<String> values = new LinkedList<String>();
			for (String s: sourceEntity.getValues().split(" "))
			{
				try
				{
					values.add (URLDecoder.decode(s, "UTF-8"));
				}
				catch (UnsupportedEncodingException e)
				{
					throw new RuntimeException (e);
				}
			}
			targetVO.setValues(values);
		}
	}

	@Override
	public void attributeToEntity(Attribute source, AttributeEntity target, boolean copyIfNull) {
		super.attributeToEntity(source, target, copyIfNull);
		StringBuffer b = new StringBuffer();
		for (String s: source.getValues())
		{
			if (b.length() > 0)
				b.append (" ");
			try
			{
				b.append (URLEncoder.encode(s, "UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException (e);
			}
		}
		target.setValues(b.toString());
	}


}
