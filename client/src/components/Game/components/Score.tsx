import { useEffect, useState } from "react";
import { doc, onSnapshot } from "firebase/firestore";
import { db } from "../../../firebase.ts";
import { useGlobalState } from "../GlobalStateProvider";
import { useUser } from "@clerk/clerk-react";

export default function Score({
  onFinish,
}: {
  onFinish: (win: boolean) => void;
}) {
  const [players, setPlayers] = useState<{ [key: string]: any }>({});
  const user = useUser().user;
  let username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];
  if (!username) {
    username = localStorage.getItem("guestName") || "";
  }
  const state = useGlobalState();

  // helper function to convert a string fraction to a number
  const strFracToNum = (score: string) => {
    const parts = score.split("/");
    return parseInt(parts[0], 10) / parseInt(parts[1], 10);
  };

  // set up a real-time listener using firebase for score updates
  useEffect(() => {
    if (!state.roomID) return;

    const docRef = doc(db, "Rooms", state.roomID);

    const unsubscribe = onSnapshot(docRef, (docSnapshot) => {
      if (docSnapshot.exists()) {
        const data = docSnapshot.data();
        console.log("snapshot");
        if (data.players) {
          console.log(data.players);
          setPlayers(data.players);
          const playerData = Object.entries(data.players).map(
            ([key, value]: [string, any]) => {
              return {
                key,
                userName: value.displayName,
                userScore: value.userScore,
              };
            }
          );
          // check if a player has reached a score of 1
          const player1 = playerData[0];
          const player2 = playerData[1];
          if (strFracToNum(player1.userScore) === 1) {
            if (player1.userName === username) {
              onFinish(true);
            } else {
              onFinish(false);
            }
          } else if (strFracToNum(player2.userScore) === 1) {
            if (player2.userName === username) {
              onFinish(true);
            } else {
              onFinish(false);
            }
          }
        }
      } else {
        console.log("Room document does not exist!");
      }
    });

    return () => unsubscribe();
  }, [state.roomID]);

  return (
    <div>
      <h1 className="flex justify-center text-2xl font-bold py-2 pb-4 font-courier">
        Score
      </h1>
      <div className="flex flex-col gap-2 pb-8 text-gray-200">
        {Object.entries(players)
          // Sort players by score (highest to lowest)
          .sort(
            ([, playerA], [, playerB]) =>
              strFracToNum(playerB.userScore) - strFracToNum(playerA.userScore)
          )
          // Map over sorted players
          .map(([key, player]) => (
            <div
              className="px-2 py-2 bg-zinc-700 text-sm rounded-md font-courier hover:bg-gray-600"
              key={key}
            >
              <strong>{player.displayName}</strong>: {player.userScore}
            </div>
          ))}
      </div>
    </div>
  );
}
