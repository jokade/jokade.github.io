import scalanative.native._

object Main {
  def main(args: Array[String]): Unit = {
    Gtk.init(null,null)

    val window = new GtkWindow(0) // 0 = top level window
    window.setTitle(c"Hello Scala Native!")
    window.setBorderWidth(10)
    window.setSizeRequest(200, 100)

    window.signalConnect(c"destroy", CFunctionPtr.fromFunction0(destroy), null)

    val label = new GtkLabel(null)
    label.setMarkup(c"<span size='large'>Hello Scala Native!</span>")

    window.add(label)
    window.showAll()

    Gtk.main()

  }

  def destroy(): Unit = {
    Gtk.mainQuit()
  }
}

trait Ref

@extern
object Gtk {
  @name("gtk_init")
  def init(argc: Ptr[Int], argv: Ptr[Ptr[CString]]): Unit = extern
  @name("gtk_main")
  def main(): Unit = extern
  @name("gtk_main_quit")
  def mainQuit(): Unit = extern
}

abstract class GObject {
  def ref: Ref
  // In GObject, the c_handler actually receives 2 args: the pointer to the object that received the signal,
  // and the pointer to a user-defined data object (which may be null)
  def signalConnect(detailed_signal: CString, c_handler: CFunctionPtr0[Unit], data: Ptr[Byte]): UInt =
    GObject.g_signal_connect_data(ref.cast[Ptr[Byte]],detailed_signal,c_handler,data,null,0)
}
@extern
object GObject {
  def g_signal_connect_data(self: Ptr[Byte], detailed_signal: CString, c_handler: CFunctionPtr0[Unit],
                            data: Ptr[Byte], destroy_data: CFunctionPtr0[Unit], connect_flags: Int): UInt = extern
}

abstract class GtkWidget extends GObject {
  def ref: Ref
  def setSizeRequest(width: Int, height: Int): Unit = GtkWidget.gtk_widget_set_size_request(ref.cast[Ptr[Byte]],width,height)
  def showAll(): Unit = GtkWidget.gtk_widget_show_all(ref.cast[Ptr[Byte]])
}
@extern
object GtkWidget {
  def gtk_widget_set_size_request(self: Ptr[Byte], width: Int, height: Int): Unit = extern
  def gtk_widget_show_all(self: Ptr[Byte]): Unit = extern
}

abstract class GtkContainer extends GtkWidget {
  def ref: Ref
  def setBorderWidth(width: Int): Unit = GtkContainer.gtk_container_set_border_width(ref.cast[Ptr[Byte]],width)
  def add(widget: GtkWidget): Unit = GtkContainer.gtk_container_add(ref.cast[Ptr[Byte]], widget.ref.cast[Ptr[Byte]])
}
@extern
object GtkContainer {
  def gtk_container_set_border_width(self: Ptr[Byte], width: Int): Unit = extern
  def gtk_container_add(self: Ptr[Byte], widget: Ptr[Byte]): Unit = extern
}

class GtkWindow private(val ref: Ref) extends GtkContainer {
  def this(windowType: Int) = this(GtkWindow.gtk_window_new(windowType).cast[Ref])
  def setTitle(title: CString): Unit = GtkWindow.gtk_window_set_title(ref.cast[Ptr[Byte]],title)
}
@extern
object GtkWindow {
  def gtk_window_new(windowType: Int): Ptr[Byte] = extern
  def gtk_window_set_title(self: Ptr[Byte], title: CString): Unit = extern
}

class GtkLabel private(val ref: Ref) extends GtkWidget {
  def this(str: CString) = this(GtkLabel.gtk_label_new(str).cast[Ref])
  def setMarkup(str: CString): Unit = GtkLabel.gtk_label_set_markup(ref.cast[Ptr[Byte]],str)
}
@extern
object GtkLabel {
  def gtk_label_new(str: CString): Ptr[Byte] = extern
  def gtk_label_set_markup(self: Ptr[Byte], str: CString): Unit = extern
}
