import AvailableGames from "./AvailableGames";
import OnlineUsers from "./OnlineUsers";
import BinaryBackground from "../Home/BinaryBackground";
import Header from "../Header";

// wrapper for all lobby logic and components, as well as create game logic
export default function WaitingRoom() {
  return (
    <div className="relative w-full h-screen flex flex-col overflow-hidden">
      <div className="flex h-[15%]">
        <Header pageTitle="Lobby" />
      </div>
      <BinaryBackground />
      <div className="flex flex-row h-[85%]">
        <div className="w-3/4 flex flex-col">
          <AvailableGames />
        </div>
        <div className="w-1/4 pl-1 flex flex-col">
          <OnlineUsers />
        </div>
      </div>
    </div>
  );
}
