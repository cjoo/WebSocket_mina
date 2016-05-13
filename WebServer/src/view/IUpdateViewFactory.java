package view;

public class IUpdateViewFactory {
	private static UpdateView updateView;

	public static UpdateView getUpdateView() {
		return updateView;
	}

	public static void setUpdateView(UpdateView updateView) {
		IUpdateViewFactory.updateView = updateView;
	}
}
