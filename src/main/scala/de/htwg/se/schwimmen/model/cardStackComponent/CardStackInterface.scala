package de.htwg.se.schwimmen.model.cardStackComponent

trait CardStackInterface {
  def setCardStack(): CardStackInterface
  def rndCardStack: List[(String, String)]
  def getThreeCards: List[(String, String)]
  def delThreeCards: CardStackInterface
}
