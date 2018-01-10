import scalanative.native._
import de.surfice.smacrotools.debug

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

@CObj
object Gtk {
  def init(argc: Ptr[Int], argv: Ptr[Ptr[CString]]): Unit = extern
  def main(): Unit = extern
  def mainQuit(): Unit = extern
}

@CObj
abstract class GObject {
  def signalConnect(detailed_signal: CString, c_handler: CFunctionPtr0[Unit], data: Ptr[Byte]): UInt =
    signalConnectData(detailed_signal,c_handler,data,null,0)

  @name("g_signal_connect_data")
  def signalConnectData(detailed_signal: CString, c_handler: CFunctionPtr0[Unit],
                        data: Ptr[Byte], destroy_data: CFunctionPtr0[Unit], connect_flags: Int): UInt = extern
}

@CObj
abstract class GtkWidget extends GObject {
  def setSizeRequest(width: Int, height: Int): Unit = extern
  def showAll(): Unit = extern
}

@CObj
abstract class GtkContainer extends GtkWidget {
  def setBorderWidth(width: Int): Unit = extern
  def add(widget: GtkWidget): Unit = extern
}

@CObj
class GtkWindow(windowType: Int) extends GtkContainer {
  def setTitle(title: CString): Unit = extern
}

@CObj
class GtkLabel(str: CString) extends GtkWidget {
  def setMarkup(str: CString): Unit = extern
}
