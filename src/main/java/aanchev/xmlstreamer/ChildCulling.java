package aanchev.xmlstreamer;

public interface ChildCulling {

	public boolean keepsChildren();
	public void keepChildren(boolean shouldKeep);

	public boolean keepsAllChildren();
	public void keepAllChildren(boolean shouldKeep);
}
