package de.htwg.se.schwimmen.aUI

import de.htwg.se.schwimmen.controller.controllerComponent.*
import de.htwg.se.schwimmen.fieldComponent.PlayerInterface
import de.htwg.se.schwimmen.model.EasyStrategy
import de.htwg.se.schwimmen.fieldComponent.*

import scala.util.{Failure, Success, Try}
import scala.swing.Reactor

class TUI(val controller: ControllerInterface) extends Reactor {

  listenTo(controller)
  controller.createNewGame()
  val strat = new EasyStrategy
  var playerCardInt = 0
  var input: String = ""
  def processInput(): String = {
    Try {input.toInt} match {
      case Success(e) =>
        if (controller.playerAmount == 0 && controller.field.processPlayerAmount(input.toInt)) {
          controller.setPlayerAmount(input.toInt)
          return "player amount set"
        } else if ((input.toInt == 1 | input.toInt == 2 | input.toInt == 3) && playerCardInt == 0) {
          playerCardInt = input.toInt
          controller.cardSelected()
          return "card selected"
        } else if ((input.toInt == 1 | input.toInt == 2 | input.toInt == 3) && playerCardInt != 0) {
          controller.swapCards(playerCardInt - 1, input.toInt - 1)
          playerCardInt = 0
          controller.nextPlayer()
          return "cards swapped and player changed"
        }
        "illegal input"
      case Failure(e) =>
        input match {
          case "saveXml" =>
            controller.saveTo("Xml")
            "saved to Xml"
          case "saveJson" =>
            controller.saveTo("Json")
            "saved to Json"
          case "loadXml" =>
            controller.loadFrom("Xml")
            "loaded from Xml"
          case "loadJson" =>
            controller.loadFrom("Json")
            "loaded from Json"
          case "nr" =>
            controller.nextRound()
            "next round"
          case "q" =>
            System.exit(0)
            "input set to q"
          case "z" =>
            controller.undo()
            "undo"
          case "r" =>
            controller.redo()
            "redo"
          case _ =>
            if (controller.playerAmount > controller.players.size) {
              controller.addPlayer(input)
              "player added"
            } else {
              input match {
                case "y" =>
                  controller.yesSelected()
                  "yes selected"
                case "all" =>
                  controller.swapAllCards()
                  controller.nextPlayer()
                  "all cards swapped and player changed"
                case "k" =>
                  controller.setKnocked()
                  controller.nextPlayer()
                  "player knock set and player changed"
                case "n" =>
                  controller.doNothing()
                  controller.nextPlayer()
                  "player changed"
                case _ =>
                  "illegal input"
              }
            }
        }
    }
  }

  reactions += {
    case event: NewGame => println(sayWelcomeString())
    case event: PlayerAmountChanged => println(typeYourNameString())
    case event: PlayerAdded => println(typeYourNameString())
    case event: YesSelected => println("which one of yours?(1/2/3)")
    case event: CardSelected => println("which one of the field?(1/2/3)")
    case event: PlayerChanged => println(nextPlayerString())
  }

  def sayWelcomeString(): String = {
    """Welcome to Schwimmen!
      |How many players want to play?
      |Possible player amount is 2-9.
      |""".stripMargin
  }

  def statsString(string: String): String = {
    if (string.equals("head")) {
      "\n" + controller.field.toString + "\n" + controller.players.head.toString + "\n"
    } else {
      "\n" + controller.field.toString + "\n" + controller.players.last.toString + "\n"
    }
  }

  def typeYourNameString(): String = {
    if (controller.playerAmount <= controller.players.size) {
      statsString("head") + firstOutputString()
    } else {
      s"\nPlayer ${controller.players.size + 1}, type your name:"
    }
  }

  def firstOutputString(): String = {
    if (controller.players.head.hasKnocked || strat.checkStop(controller.players.last)) return endOfGameStats()
    val playerName = controller.players.head.name match {
      case Some(s) => s
      case None => ""
    }
    s"\n${playerName}, its your turn! " +
      s"Do you want to change a card?(y/n) or all cards?(all) or knock?(k)"
  }

  def nextPlayerString(): String = {
    statsString("last") + statsString(controller.players.head) + firstOutputString()
  }

  def endOfGameStats(): String = {
    val res = controller.players.sortBy(_.cardCount).reverse
    val builder = new StringBuilder
    var looseList:List[PlayerInterface] = Nil
    res foreach(pl => if (pl.cardCount == res.last.cardCount) looseList = looseList.::(pl))
    controller.players = res.dropRight(looseList.size)
    looseList foreach { pl =>
      if (pl.life - 1 == -1) {
        val playerName = pl.name match {
          case Some(s) => s
          case None => ""
        }
        controller.playerAmount = controller.playerAmount - 1
        builder.append(playerName).append(" you're out")
      } else
        controller.players = controller.players.::(pl.setLife(pl.life - 1))
    }
    res.foreach(pl => {
      val playerName = pl.name match {
        case Some(s) => s
        case None => ""
      }
      builder.append(s"\n${playerName}:    ${pl.cardCount} points    ${pl.life} lives left")
    })
    builder.append("\n")
    looseList.foreach(pl => {
      val playerName = pl.name match {
        case Some(s) => s
        case None => ""
      }
      builder.append(playerName).append(", ")
    })
    builder.append("lost a Life").append("\n")
    if (controller.playerAmount == 1) {
      val playerName = controller.players.head.name match {
        case Some(s) => s
        case None => ""
      }
      builder.append(playerName).append(", Congratulations you've won the game:)")
      System.exit(0)
    } else {
      builder.append("start next round with(nr)")
    }
    builder.toString()
  }
}
