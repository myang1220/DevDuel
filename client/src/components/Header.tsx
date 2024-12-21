import { useNavigate } from "react-router-dom";
import { useClerk, SignedIn, SignedOut } from "@clerk/clerk-react";
import { useState } from "react";

// returns a header component
export default function Header({ pageTitle }: { pageTitle: string }) {
  const navigate = useNavigate();
  const { signOut } = useClerk();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const isUser = useClerk().user;
  const [showDialog, setShowDialog] = useState(false);

  // handles leaving the game if in a game
  function handleLeave() {
    const guestName = localStorage.getItem("guestName");
    localStorage.clear();
    if (guestName) {
      localStorage.setItem("guestName", guestName);
    }
    navigate("/lobby");
  }

  // handles canceling the leave game dialog
  const handleCancel = () => {
    toggleDropdown();
    setShowDialog(false);
  };

  // handles confirming the leave game dialog
  const handleOk = () => {
    setShowDialog(false);
    handleLeave();
  };

  // handles signing out the user
  const handleSignOut = async () => {
    if (!isUser) {
      navigate("/");
      return;
    }
    try {
      await signOut();
    } catch (error) {
      console.error("Error signing out:", error);
    }
  };

  // dropdown for menu
  const toggleDropdown = () => {
    setDropdownOpen(!dropdownOpen);
  };

  return (
    <header className="w-full h-full shadow-md pb-1 relative bg-gradient-to-r from-yellow-400 to-pink-400 bg-opacity-40 z-20">
      {showDialog === true && (
        <div className="fixed top-0 left-0 w-full h-full bg-black bg-opacity-50 flex justify-center items-center">
          <div className="bg-gray-600 rounded-lg p-12 shadow-lg text-montserrat text-gray-200">
            <p>
              Leaving the game is detrimental to the experience of other
              players. Are you sure you want to continue?
            </p>
            <div className="flex justify-end gap-2 mt-12">
              <button
                onClick={handleCancel}
                className="px-4 py-2 bg-gray-500 rounded hover:bg-gray-400"
              >
                Cancel
              </button>
              <button
                onClick={handleOk}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-500"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}
      <div className="h-full flex flex-col justify-center items-center p-8 bg-black shadow-md relative">
        {/* Page Title */}
        <div className="absolute left-20 top-1/2 transform -translate-y-1/2">
          <h2 className="text-4xl font-courier text-gray-400 font-bold">
            {pageTitle}
          </h2>
        </div>

        {/* Centered Title */}
        <h1 className="text-7xl font-bold text-gray-400 font-courier z-10 tracking-wider text-glow text-center">
          DEVDUEL
        </h1>

        {/* User Dropdown */}
        <div className="absolute right-20 top-1/2 transform -translate-y-1/2">
          <button
            className="relative text-2xl font-courier text-gray-400 font-bold rounded-xl shadow-lg bg-gradient-to-r from-yellow-400 to-pink-400 p-[2px]"
            onClick={toggleDropdown}
          >
            <div className="h-full bg-black px-4 py-2 rounded-xl flex items-center justify-center hover:bg-gray-600">
              User
            </div>
          </button>
          {dropdownOpen && (
            <div className="absolute left-1/2 transform -translate-x-1/2 mt-2 w-48 bg-black border border-gray-700 rounded-md shadow-lg z-20">
              <ul className="py-1 text-gray-400">
                {pageTitle === "Lobby" && isUser && (
                  <li
                    className="block px-4 py-2 hover:bg-gray-600 cursor-pointer"
                    onClick={() => navigate("/profile")}
                  >
                    Profile
                  </li>
                )}
                {pageTitle === "Profile" && isUser && (
                  <li
                    className="block px-4 py-2 hover:bg-gray-600 cursor-pointer"
                    onClick={() => {
                      navigate("/lobby");
                    }}
                  >
                    Lobby
                  </li>
                )}
                {pageTitle !== "Profile" && pageTitle !== "Lobby" && (
                  <li
                    className="block px-4 py-2 hover:bg-gray-600 cursor-pointer"
                    onClick={() => setShowDialog(true)}
                  >
                    Leave Game
                  </li>
                )}

                <SignedIn>
                  {(pageTitle === "Profile" || pageTitle === "Lobby") && (
                    <li
                      className="block px-4 py-2 hover:bg-gray-600 cursor-pointer"
                      onClick={handleSignOut}
                    >
                      Sign Out
                    </li>
                  )}
                </SignedIn>
                <SignedOut>
                  {(pageTitle === "Profile" || pageTitle === "Lobby") && (
                    <li
                      className="block px-4 py-2 hover:bg-gray-600 cursor-pointer"
                      onClick={() => navigate("/")}
                    >
                      Sign Out
                    </li>
                  )}
                </SignedOut>
              </ul>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
