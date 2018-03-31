package blog.pprogs.ktUnionClient

import blog.pprogs.ktunion.Command
import blog.pprogs.ktunion.Context
import blog.pprogs.ktunion.UnionClient
import groovy.lang.Binding
import groovy.lang.GroovyShell
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import okhttp3.OkHttpClient
import okhttp3.Request
import tornadofx.*
import java.io.File

class MessageView(val author: String?, val content: String) : Fragment() {
    override val root = vbox {
        style {
            paddingLeft = 10
            if (author != null) {
                paddingTop = 10
            }
        }
        if (author != null) {
            label(author) {
                style {
                    fontWeight = FontWeight.EXTRA_BOLD
                    fontFamily = "Roboto"
                    fontSize = Dimension(20.0, Dimension.LinearUnits.pt)
                }
                graphic
            }
        }
        label(content) {
            isWrapText = true
        }


    }
}

class KtUnionClientView : View() {
    private val messageLabel = SimpleStringProperty("Started")
    val creds = File("creds.txt").readLines()
    val client: UnionClient = UnionClient(selfbot = true, username = creds[0], password = creds[1])
    var realMessageList: VBox by singleAssign()
    var scroll: ScrollPane by singleAssign()
    var last: String = ""

    override val root = borderpane {

        paddingAll = 0

        scroll = scrollpane {
            realMessageList = vbox {}
            isFitToWidth = true

            style {
                useMaxWidth = true
            }
        }
        center = scroll

        bottom = textfield {
            style {
                useMaxWidth = true
            }
            action {
                var value = text
                if (value.startsWith("/tableflip"))
                    value = value.substring(11) + " (╯°□°）╯︵ ┻━┻"
                else if (value.startsWith("/clear")) {
                    realMessageList.clear()
                    value = ""
                    last = ""
                }

                if (value.isNotBlank()) {
                    client.sendMessage(value)
                }
                selectAll()
            }
        }


    }

    init {
        client.onTextMessage = { who, content, _ ->
            runLater {

                if (last == who) {
                    realMessageList.add(MessageView(null, content))
                } else {
                    realMessageList.add(MessageView(who, content))
                }
                last = who
                scroll.vvalue = 1.0
            }
        }
        client.addCommand(Command("hello", "Says hello")) {
            client.sendMessage("Why are you talking to yourself.")
        }

        client.addCommand(Command("mock", "Mocks u")) {
            client.sendMessage(
                    it.args.joinToString(" ").map {
                        if (Math.random() > .5) it.toUpperCase() else it.toLowerCase()
                    }.joinToString(""))
        }

        client.addCommand(Command("eval", "Evals some code")) {
            val shell = createShell(it, this)
            try {
                val result = shell.evaluate(it.args.joinToString(" "))
                if (result == null) {
                    client.sendMessage("👌")
                } else {
                    client.sendMessage(result.toString())
                }
            } catch (e: Exception) {
                client.sendMessage("Error:\n\n$e")
            }
        }

        client.addCommand(Command("weather", "Send the weather!")) {
            val response = OkHttpClient().newCall(Request.Builder()
                    .url("http://wttr.in/?TQn0")
                    .header("User-Agent", "curl")
                    .build()).execute().body()!!.string()
            it.reply("​\n$response")
        }


        setWindowMinSize(300, 150)

        client.start()
    }

    private fun createShell(ctx: Context, view: View): GroovyShell {
        val binding = Binding().apply {
            setVariable("client", ctx.client)
            setVariable("args", ctx.args)
            setVariable("command", ctx.command)
            setVariable("cmd", ctx.command)
            setVariable("sender", ctx.sender)
            setVariable("author", ctx.sender)
            setVariable("server", ctx.server)
            setVariable("socket", ctx.socket)
            setVariable("ctx", ctx)
            setVariable("data", ctx.data)
            setVariable("view", view)
        }

        return GroovyShell(binding)
    }

}


class KtUnionClientApp : App(KtUnionClientView::class)

fun main(args: Array<String>) = Application.launch(KtUnionClientApp::class.java, *args)