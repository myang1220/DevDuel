import { useNavigate } from "react-router-dom";
import { queryAPI } from "../../api.ts";
import { useUser } from "@clerk/clerk-react";

type GamePanelProps = {
  name: string;
  roomID: string;
  difficulty: string;
  time: string;
};

// game panel advertising an available game
export default function GamePanel({
  name,
  roomID,
  difficulty,
  time,
}: GamePanelProps) {
  const navigate = useNavigate();
  const user = useUser().user;
  let username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];
  if (!username) {
    username = localStorage.getItem("guestName") || "";
  }

  // add user to room and navigate to countdown page
  async function addUser() {
    navigate("/countdown", { state: { roomID } });
    try {
      const response = await queryAPI("RoomSet", {
        roomID,
        userName: username as string,
        userID: user?.id || username || "",
        userScore: "0/10",
        timeSubmitted: new Date().toISOString(),
      });
      if (response.response_type === "success") {
        console.log("User added to room:", response.data);
      } else {
        console.error("Failed to add user to room:", response);
      }
    } catch (error) {
      console.error("Error adding user to room:", error);
    }
  }

  return (
    <div className="relative font-courier text-gray-500 font-bold rounded-xl shadow-2xl w-1/5 bg-gray-300 p-[1px]">
      <div className="flex flex-col h-full w-full bg-black py-6 rounded-xl">
        <h3 className="flex justify-center pb-8 px-4 text-lg text-gray-300 text-center break-words">
          {name}'s Game
        </h3>

        <div className="flex flex-col align-left px-4 gap-4 pb-4 text-sm font-normal">
          <p>
            <b className="font-bold text-gray-400">Difficulty: </b>
            {difficulty}
          </p>
          <p>
            <b className="font-bold text-gray-400">Time: </b>
            {time}
          </p>
        </div>

        <div className="flex justify-center mt-auto">
          <button className="relative font-courier text-gray-400 font-bold rounded-xl shadow-lg w-1/2 bg-gray-300 p-[1px] hover:shadow-xl">
            <div
              className="h-full w-full bg-stone-800 py-3 rounded-xl hover:bg-gray-700"
              onClick={() => addUser()}
            >
              Join!
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}
