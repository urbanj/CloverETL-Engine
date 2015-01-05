/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.component;

import java.util.Properties;

import org.jetel.exception.JetelRuntimeException;


/**
 * The only abstract implementation of {@link GenericTransform} interface.
 * 
 * @author Kokon (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created 20. 11. 2014
 */
public abstract class AbstractGenericTransform extends AbstractDataTransform implements GenericTransform {

	private Properties additionalAttributes;
	
	@Override
	public void setAdditionalAttributes(Properties additionalAttributes) {
		this.additionalAttributes = additionalAttributes;
	}

	public Properties getAdditionalAttributes() {
		return additionalAttributes;
	}
	
	@Override
	public void init() {
	}

	@Override
	public abstract void execute();
	
	@Override
	public void executeOnError(Exception e) {
		throw new JetelRuntimeException("Transform failed!", e);
	}

	@Override
	public void free() {
	}
	
}