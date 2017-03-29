package aa5.ecs.soton.ac.uk.xmlstreamer;

import java.util.Collection;

public interface Element {
	public String getTag();
	public String getText();
	public Object getAttribute(String attr);
	public Collection<Element> getChildren();
	public boolean isClosed();
}