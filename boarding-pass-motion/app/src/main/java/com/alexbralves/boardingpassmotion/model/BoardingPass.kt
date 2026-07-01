package com.alexbralves.boardingpassmotion.model

data class BoardingPass(
  val airline: String = "Jetpack Airlines",
  val passengerName: String = "Rich Miner",
  val originCode: String = "GRU",
  val originCity: String = "Sao Paulo",
  val destinationCode: String = "DXB",
  val destinationCity: String = "Dubai",
  val flightNumber: String = "AU 2048",
  val gate: String = "B18",
  val terminal: String = "3",
  val seat: String = "04A",
  val group: String = "A",
  val date: String = "JUL 18",
  val time: String = "22:45",
  val boardingTime: String = "22:25",
  val flightDuration: String = "14Hr 15Min",
  val boardingStatus: String = "Boarding",
  val boardingCloses: String = "Boarding closes 22:25",
  val sequence: String = "SEQ 018",
  val cabin: String = "Business",
)
