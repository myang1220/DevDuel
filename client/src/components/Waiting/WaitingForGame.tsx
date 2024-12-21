import BinaryBackground from "../Home/BinaryBackground";
import { useLocation, useNavigate } from "react-router-dom";
import { queryAPI } from "../../api";
import { doc, onSnapshot } from "firebase/firestore";
import { useEffect } from "react";
import { db } from "../../firebase";

// page for a user that is waiting for a game to be loaded
export default function WaitingForGame() {
  const location = useLocation();
  const navigate = useNavigate();
  const { roomID } = location.state || {};

  // use RoomDel to delete the room if the game is canceled
  async function endGame() {
    navigate("/lobby");
    try {
      const response = await queryAPI("RoomDel", {
        roomID: roomID,
      });
      if (response.response_type === "success") {
        console.log("Room deleted successfully");
      } else {
        console.error("Failed to delete room");
      }
    } catch (error) {
      console.error("Error deleting room:", error);
    }
  }

  useEffect(() => {
    if (!roomID) return;

    // Reference to the specific room document
    const docRef = doc(db, "Rooms", roomID);

    // Set up a real-time listener for the room document
    const unsubscribe = onSnapshot(docRef, (docSnapshot) => {
      if (docSnapshot.exists()) {
        const data = docSnapshot.data();

        // Check for players field in the document
        if (data.players) {
          const playersData = data.players;

          // Check if the players object contains exactly two users
          if (Object.keys(playersData).length === 2) {
            navigate("/countdown", { state: { roomID } });
          }
        }
      } else {
        console.log("Room document does not exist!");
      }
    });

    // Cleanup listener when component unmounts
    return () => unsubscribe();
  }, [roomID]);

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-30 z-20">
      <BinaryBackground />
      <div className="relative bg-gray-700 opacity-90 text-white rounded-xl shadow-lg w-{45%} p-8 flex flex-col items-center">
        {/* Title */}
        <h1 className="text-3xl font-bold mb-6 text-center font-courier text-gray-400">
          Waiting for a Player to Join...
        </h1>

        {/* Loading Animation */}
        <div className="flex flex-col items-center justify-center">
          <img
            src="./loading5.gif"
            alt="loading animation"
            width="30%"
            height="30%"
            className="opacity-80 mt-6"
          />
        </div>
        <button
          className="mt-10 flex font-courier text-gray-400 rounded-xl shadow-lg bg-red-500 p-[1px] hover:shadow-xl"
          onClick={() => endGame()}
        >
          <div className="flex justify-center bg-red-700 py-3 px-4 rounded-xl shadow-lg hover:bg-red-800 text-xl font-bold">
            Cancel
          </div>
        </button>
      </div>
    </div>
  );
}
