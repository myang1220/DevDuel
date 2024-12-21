import Header from "../Header";
import PastCode from "./PastCode";
import OnlineUsers from "./Stats";
import BinaryBackground from "../Home/BinaryBackground";

// wrapper for profile logic and components
export default function ProfilePage() {
  return (
    <div className='relative w-full h-screen flex flex-col overflow-hidden'>
      <div className='flex h-[15%]'>
        <Header pageTitle="Profile" />
      </div>
      <BinaryBackground />
      <div className='flex flex-row h-[85%]'>
        <div className='w-3/4 flex flex-col'>
          <PastCode />
        </div>
        <div className='w-1/4 pl-1 flex flex-col'>
          <OnlineUsers />
        </div>
      </div>
    </div>
  );
}
