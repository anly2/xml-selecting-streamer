package aanchev.xmlstreamer;

public interface ChildCullingXMLStreamer {

	public boolean keepsChildren();
	public void keepChildren(boolean shouldKeep);

	public boolean keepsAllChildren();
	public void keepAllChildren(boolean shouldKeep);
}
