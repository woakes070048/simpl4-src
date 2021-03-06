/**
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
package org.ms123.common.camel.components.asciidoctor;

import java.io.StringWriter;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.component.ResourceEndpoint;
import org.apache.camel.util.ExchangeHelper;
import org.ms123.common.camel.api.ExchangeUtils;
import org.ms123.common.docbook.DocbookService;
import static com.jcabi.log.Logger.info;

@SuppressWarnings("unchecked")
public class AsciidoctorEndpoint extends ResourceEndpoint {

	private String output = "html";
	private String source = null ;
	private String destination = null ;
	private boolean enclosingDiv=false;

	public AsciidoctorEndpoint() {
	}

	public AsciidoctorEndpoint(String endpointUri, Component component, String resourceUri) {
		super(endpointUri, component, resourceUri);
		info(this,"AsciidoctorEndpoint:endpointUri:" + endpointUri + "/resourceUri:" + resourceUri);
	}

	public void setOutput(String type){
		 this.output = type;
	}

	public String getOutput(){
		 return this.output;
	}

	public void setDestination(String o){
		 this.destination = o;
	}

	public String getDestination(){
		 return this.destination;
	}

	public void setSource(String o){
		 this.source = o;
	}

	public String getSource(){
		 return this.source;
	}

	public void setEnclosingDiv(boolean b){
		 this.enclosingDiv = b;
	}

	public boolean isEnclosingDiv(){
		 return this.enclosingDiv;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public ExchangePattern getExchangePattern() {
		return ExchangePattern.InOut;
	}

	@Override
	protected void onExchange(Exchange exchange) throws Exception {
		String text = ExchangeUtils.getSource(this.source, exchange, String.class);
		DocbookService ds = getDocbookService();

		Message out = exchange.getIn();
		if( "html".equals(this.output)){
			String html = ds.adocToHtml( text);
			if( isEnclosingDiv()){
				html = "<div>" + html + "</div>";
			}
		  ExchangeUtils.setDestination(this.destination,html , exchange);
		}else{
			String docbook = ds.adocToDocbook( text);
		  ExchangeUtils.setDestination(this.destination,docbook , exchange);
		}

	}

	public DocbookService getDocbookService() {
		return getByType(DocbookService.class);
	}

	private <T> T getByType(Class<T> kls) {
		return kls.cast(getCamelContext().getRegistry().lookupByName(kls.getName()));
	}

}
