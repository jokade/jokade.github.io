---
title: Object-Oriented Scala Native Bindings to Gtk+
description: A small example that shows how idiomatic bindings to Gtk+ can be accomplished in Scala Native.
photos:
 - /assets/images/posts/2018-01-10/screenshot.png
categories:
 - Scala Native
tags: tutorial scala-native gtk+
---

<!-- A small example that shows how idiomatic bindings to [Gtk+](http://www.gtk.org) can be achieved in [Scala Native](http://www.scala-native.org).-->

Alright, that's not too impressive... but it's sufficient to explore the basic concepts required to make an object-oriented
[Scala Native](http://www.scala-native.org) binding to [Gtk+](http://www.gtk.org).

## Example in C
Here's what this example looks like in C:

```c
#include <gtk/gtk.h>

// callback used to close the window and exit the application
static void destroy (GtkWidget*, gpointer);

int main(int argc, char* argv[]) {
  GtkWidget *window, *label
  
  gtk_init(&argc, &argv);
  
  /* Create and configure main window */
  window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title(GTK_WINDOW(window), "Hello Scala Native!");
  gtk_container_set_border_width(GTK_CONTAINER(window), 10);
  gtk_widget_set_size_request(window, 200, 100);
  
  /* Connect the main window to the 'destroy' signal.
   * This signal is emitted when we click on the window 'close' button. */
  g_signal_connect(G_OBJECT(window), "destroy", G_CALLBACK(destroy), NULL);
  
  /* Create a label */
  label = gtk_label_new(NULL);
  gtk_label_set_markup("<span size='large'>Hello Scala Native!</span>");
  
  /* Add the label as a child of the window */
  gtk_container_add(GTK_CONTAINER(window), label);
  
  /* Display window and all of its descendants */
  gtk_widget_show_all(window);
  
  /* Enter main loop */
  gtk_main();
  return 0;
}

void destroy(GtkWidget* widget, gpointer data) {
  gtk_main_quit();
}
```

If you're not familiar with Gtk+, here are some explanations:
- First we must initialize the library. In doing so, we can pass the command line arguments to Gtk+
  which then will automatically handle all recognized arguments and remove them from the `argv` array.
  
- Next we create a new toplevel window, set the window title, the border width (an inner padding between window
  border and its child), and request a preferred window size. We need the calls to `GTK_WINDOW()` and `GTK_CONTAINER()`{% fn %} 
  to cast `window` to the expected type.
  
- Then we register an event handler (Gtk+ calls them "signals") so that we can exit the application when the user clicks
  on the window close button.
  
- We create a new label to display some text (using Pango markup language, which is similar to HTML), and add the new label
  as child to our window.
  
- Finally we set all widgets to be visible and enter enter Gtk+ main event loop. Note that this loop won't exit
  until we call `gtk_main_quit`, which happens in our `destroy` callback.

## Create Object-Oriented Bindings
### The Gtk+ C API
Since Gtk+ is a pure C library, we can easily create an `@extern` binding object for it and then
translate the example above line by line to Scala Native, e.g.:

```scala
import scalanative.native._

@extern
object gtk {
  def gtk_init(argc: Int, argv: Ptr[CString]): Unit = extern
  def gtk_window_new(windowType: Int): Ptr[Byte] = extern
  def gtk_window_set_title(window: Ptr[Byte], title: CString): Unit = extern
  def gtk_container_set_border_width(container: Ptr[Byte], width: Int): Unit = extern
    ...
}

object Main {
  import gtk._
  
  def main(args: Array[String]): Unit = {
    gtk_init(0,null)   
    
    val window = gtk_window_new(0)
    gtk_window_set_title(window, c"Hello Scala Native")
      ...
  }
}
```

However, Gtk+ is an object-oriented library{% fn %}: we have some global functions (`gtk_init`, `gtk_main`),
but the bulk are either
- object constructors (`gtk_window_new`, `gtk_label_new`),
- or operate on a previously created object, which is always passed as the first argument to the function
  (`gtk_window_set_title`, `gtk_container_set_border_width`, ...). These are nothing else than instance methods in Scala,
  with the exception of the different call syntax.

The naming conventions of Gtk+ indicate on which type of object a function operates. Obviously, there are
objects of type `GtkWindow` and `GtkLabel` (we can construct both using a corresponding `_new` function). Furthermore,
 since we can cast a `GtkWindow` to a `GtkContainer` and then call `gtk_container_set_border_width` on it,
 `GtkWindow` seems to be a subtype of `GtkContainer`. And both, `window` and `label` are `GtkWidget`s (we know that since
 they were declared as such in the C example :).
 
Actually, `GtkWidget` is not the immediate parent of `GtkWindow` and `GtkLabel. Here's their full genealogy:

{% mermaid %}
graph TD
  GObject[GObject *] --> GInitiallyUnowned
  GInitiallyUnowned --> GtkObject
  GtkObject --> GtkWidget[GtkWidget *]
  GtkWidget --> GtkContainer[GtkContainer *]
  GtkWidget --> GtkMisc
  GtkContainer --> GtkBin
  GtkBin --> GtkWindow[GtkWindow *]
  GtkMisc --> GtkLabel[GtkLabel *]
{% endmermaid %}

We'll only consider the types marked with *, and ignore the rest of them for this example.

There's one more function in our example, that takes an object as its first argument: `g_signal_connect()` registers an
event handler for any `GObject`. Hence, despite it's name prefix `g_signal_`, we can consider it as an instance method of `GObject.

### Modelling the Gtk+ Types in Scala
Since we're using Scala, we want to get rid of that nasty casting business, so let's make that Gtk+ type hierarchy
explicit in Scala. Here's what a first draft of our Scala API to Gtk+ could look like: 

```scala

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
  // In GObject, the c_handler actually receives 2 args: the pointer to the object that received the signal,
  // and the pointer to a user-defined data object (which may be null)
  def signalConnect(detailed_signal: CString, c_handler: CFunctionPtr0[Unit], data: Ptr[Byte]): UInt = ???
}

abstract class GtkWidget extends GObject {
  def setSizeRequest(width: Int, height: Int): Unit = ???
  def showAll(): Unit = ???
}

abstract class GtkContainer extends GtkWidget {
  def setBorderWidth(width: Int): Unit = ???
  def add(widget: GtkWidget): Unit = ???
}

class GtkWindow(windowType: Int) extends GtkContainer {
  def setTitle(title: CString): Unit = ???
}

class GtkLabel(str: CString) extends GtkWidget {
  def setMarkup(str: CString): Unit = ???
}
```
You'll notice that I've translated the snake_case function names to camelCase method names, and stripped the class suffix
from the names. All classes are abstract except `GtkWindow` and `GtkLabel`, which are the only ones we can actually
instantiate.

And here is the `main` of our example translated to this Scala API:
```scala
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
```

### Implement Bindings
Let's start with the binding implementation for `GtkLabel`. We'd like to use it as a normal Scala class and
create new instances using the `new` operator. To this end we need to call `gtk_label_new()` from the primary constructor.
But what should we do with the instance pointer returned by the C function? We need to store it with our scala instance,
since we must pass it back to Gtk+ every time we call an instance method on our object.

One solution to this problem is to replace the currently defined primary constructor with one that takes the C pointer
as its only argument, and then define a secondary constructor that has the parameter signature of `gtk_label_new`:

```scala
trait Ref

class GtkLabel private(val ref: Ref) extends GtkWidget {
  def this(str: CString) = this(GtkLabel.gtk_label_new(str).cast[Ref])
  def setMarkup(str: CString): Unit = ???
}
@extern
object GtkLabel {
  def gtk_label_new(str: CString): Ptr[Byte] = extern
}
```
This snippet requires some explanations:
- We put all external declarations for a Gtk+ "class" in its companion object.

- The type of the external C reference `ref` to our Gtk+ object is a special marker trait `Ref`,
  not `Ptr[Byte]` (as one would expect, since the return type of `gtk_label_new()` is `Ptr[Byte]`).
  The reason for this becomes clear when we consider the signature of the secondary constructor: it takes one argument
  of type `CString`. But `CString` is an alias for `Ptr[Byte]` -- so we would have two constructors with identical
  signatures (which is not possible, of course). Hence the special marker trait `Ref`, which we know will never occur in
  the signatures of the C API. 
  
- We make the primary constructor private to avoid accidentially using it directly.

The implementation of the instance method `setMarkup` is then straightforward:
```scala
class GtkLabel private(val ref: Ref) extends GtkWidget {
  ...
  def setMarkup(str: CString): Unit = GtkLabel
    .gtk_label_set_markup(ref.cast[Ptr[Byte]],str)
}
@extern
object GtkLabel {
  ...
  def gtk_label_set_markup(self: Ptr[Byte], str: CString): Unit = extern
}
```

Next we implement the parent class `GtkWidget`, where we don't define the special primary constructor, but instead declare
an abstract `ref`:
```scala
abstract class GtkWidget extends GObject {
  def ref: Ref
  
  def setSizeRequest(width: Int, height: Int): Unit = GtkWidget
    .gtk_widget_set_size_request(ref.cast[Ptr[Byte]],width,height)
    
  def showAll(): Unit = GtkWidget.gtk_widget_show_all(ref.cast[Ptr[Byte]])
}
@extern
object GtkWidget {
  def gtk_widget_set_size_request(self: Ptr[Byte], width: Int, height: Int): Unit = extern
  def gtk_widget_show_all(self: Ptr[Byte]): Unit = extern
}
```
The implementation for `GtkContainer` is analogous, with one notable exception: in the `add()` method we must
pass the C `ref` of the widget to Gtk+, not the Scal aobject (which is the reason we made `ref` a public `val`): 
```scala
abstract class GtkContainer extends GtkWidget {
  def ref: Ref
  def setBorderWidth(width: Int): Unit = GtkContainer
    .gtk_container_set_border_width(ref.cast[Ptr[Byte]],width)
    
  def add(widget: GtkWidget): Unit = GtkContainer
    .gtk_container_add(ref.cast[Ptr[Byte]], widget.ref.cast[Ptr[Byte]])
}
@extern
object GtkContainer {
  def gtk_container_set_border_width(self: Ptr[Byte], width: Int): Unit = extern
  def gtk_container_add(self: Ptr[Byte], widget: Ptr[Byte]): Unit = extern
}
```

The implementation of `GObject.signalConnect` deviates from the standard pattern: `gtk_signal_connect` is actually
a macro in Gtk+, so we need to call `gtk_signal_connect_data` instead (which is what the C macro does):
```scala
abstract class GObject {
  def ref: Ref
  // In GObject, the c_handler actually receives 2 args: the pointer to the object that received the signal,
  // and the pointer to a user-defined data object (which may be null)
  def signalConnect(detailed_signal: CString, c_handler: CFunctionPtr0[Unit],
                    data: Ptr[Byte]): UInt =
    GObject.g_signal_connect_data(ref.cast[Ptr[Byte]],detailed_signal,
                                  c_handler,data,null,0)
}
@extern
object GObject {
  def g_signal_connect_data(self: Ptr[Byte],
                            detailed_signal: CString,
                            c_handler: CFunctionPtr0[Unit],
                            data: Ptr[Byte],
                            destroy_data: CFunctionPtr0[Unit],
                            connect_flags: Int): UInt = extern
}
```

And we're done! You can view the [complete sample code](http://github.com/jokade/jokade.github.io/blob/master/code/posts/2018-01-10/manual/src/main/scala/main.scala),
or checkout the [complete project](https://github.com/jokade/jokade.github.io/blob/master/code/posts/2018-01-10) and
run it with `sbt manual/run`.


## Generate Binding Code Automatically
Phew! Our example runs, but hacking in all that binding code is a little bit tedious, especially since most of it can be
derived from the class declaration using some simple rules. The experimental [scalanative-obj-interop](https://github.com/jokade/scalanative-obj-interop)
project does exactly that. We just need to specify the interface to the Gtk+ types:

```scala
@CObj
object Gtk {
  def init(argc: Ptr[Int], argv: Ptr[Ptr[CString]]): Unit = extern
  def main(): Unit = extern
  def mainQuit(): Unit = extern
}

@CObj
abstract class GObject {
  def signalConnect(detailed_signal: CString, c_handler: CFunctionPtr0[Unit],
                    data: Ptr[Byte]): UInt =
    signalConnectData(detailed_signal,c_handler,data,null,0)

  @name("g_signal_connect_data")
  def signalConnectData(detailed_signal: CString, c_handler: CFunctionPtr0[Unit],
                        data: Ptr[Byte], destroy_data: CFunctionPtr0[Unit],
                        connect_flags: Int): UInt = extern
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
```

and the rest will be generated during compilation by the `@CObj` annotation.

If you like to try it, check out the [sample project](http://github.com/jokade/jokade.github.io/blob/master/code/posts/2018-01-10)
and run `sbt generated/run`. If you'd lik to inspect the generated code, you can add a `@debug` annotation to a class,
and the expanded class + companion object are printed out to the console during compilation:

```scala
import de.surfice.smacrotools.debug

@CObj
@debug
abstract class GObject {
  ...
}
```  

Of course, you don't need to create the Gtk+ bindings yourself: the [scalantive-gtk project](https://github.com/jokade/scalanative-gtk)
intends to provide bindings for GLib, Gtk+, and possibly other libraries from the GNOME project (but it's still in a very early stage).


*That's it -- have fun coding Gtk+ apps in Scala!*

---
{% footnotes %}
  {% fnbody %}
  Actually, these are macros.
  {% endfnbody %}
  {% fnbody %}
  Gtk+ is based on GObject, a pure C framework for object-oriented programming with single-inheritance classes.
  {% endfnbody %}
{% endfootnotes %}