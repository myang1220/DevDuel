import GamePanel from "./GamePanel";
import CreateGame from "./CreateGame";
import { useEffect, useRef, useState } from "react";
import { queryAPI } from "../../api";

// shows all available games
export default function AvailableGames() {
  const [creatingGame, setCreatingGame] = useState(false);
  const [rooms, setRooms] = useState<Array<{
    roomID: string;
    roomName: string;
    difficulty: string;
    duration: number;
    status: string;
    players?: {
      [key: string]: {
        displayName: string;
        userScore: string;
        timeSubmitted: string;
      };
    };
  }> | null>(null);
  const [isActive, setIsActive] = useState(true);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Fetch room data from the backend
  const fetchRooms = async () => {
    try {
      const response = await queryAPI("RoomList");
      if (response.response_type === "success") {
        // Filter out rooms with zero players
        const validRooms = response.data.filter((room: any) => {
          if (
            !room.players ||
            Object.keys(room.players).length === 0 ||
            (Object.keys(room.players).length === 2 &&
              new Date(room.timeCreated).getTime() <
                new Date().getTime() - 21600000)
          ) {
            // Call RoomDel to delete the room with zero players
            queryAPI("RoomDel", { roomID: room.roomID })
              .then(() => console.log(`Room ${room.roomID} deleted.`))
              .catch((err) =>
                console.error(`Failed to delete room ${room.roomID}:`, err)
              );
            return false; // Exclude this room from the final list
          }
          return true; // Include the room
        });

        setRooms(validRooms);
        console.log("Rooms loaded:", response.data);
      } else {
        console.error("Failed to fetch rooms:", response);
      }
    } catch (error) {
      console.error("Error connecting to backend:", error);
    }
  };

  // Use useEffect to load room data on component mount and set up interval
  useEffect(() => {
    fetchRooms();

    const handleUserActivity = () => {
      setIsActive(true);
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      timeoutRef.current = setTimeout(() => {
        setIsActive(false);
      }, 60000); // 1 minute of inactivity
    };

    const events = ["mousemove", "keydown", "scroll", "click"];
    events.forEach((event) =>
      window.addEventListener(event, handleUserActivity)
    );

    intervalRef.current = setInterval(() => {
      if (isActive) {
        fetchRooms();
      }
    }, 5000); // Refresh every 5 seconds

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      events.forEach((event) =>
        window.removeEventListener(event, handleUserActivity)
      );
    };
  }, [isActive]);

  // helper function to format time in minutes and seconds
  function formatTime(time: number) {
    const minutes = Math.floor(time / 60);
    const seconds = time % 60;
    return `${minutes}m ${seconds}s`;
  }

  return (
    <div className="w-full h-full flex flex-col">
      <div className="flex-grow overflow-y-auto p-8 overscroll-none waitingroom-scrollbar">
        <div className="flex flex-wrap justify-around gap-8 items-stretch">
          {rooms ? (
            rooms
              .filter(
                (room) => room.players && Object.keys(room.players).length == 1
              )
              .map((room) => (
                <GamePanel
                  key={room.roomID}
                  roomID={room.roomID}
                  name={room.roomName}
                  difficulty={room.difficulty}
                  time={formatTime(room.duration)}
                />
              ))
          ) : (
            <p className="text-white">Loading rooms...</p>
          )}
        </div>
        <div className="flex justify-end translate-y-4 z-20">
          <button
            className="relative rounded-full p-[2px] bg-gradient-to-r from-yellow-400 to-pink-400 shadow-lg"
            onClick={() => setCreatingGame(true)}
          >
            <div className="flex items-center justify-center w-20 h-20 bg-black rounded-full hover:bg-gray-600">
              <span className="text-green-400 text-7xl font-bold neon-text leading-none -translate-y-1.5">
                +
              </span>
            </div>
          </button>
        </div>
      </div>
      {creatingGame && <CreateGame onClose={() => setCreatingGame(false)} />}
    </div>
  );
}
