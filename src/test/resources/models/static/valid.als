sig Person {}
sig Room {}

one sig Map {
  visits: Person -> Room
}

run { some Map.visits } for 2
