import { useUser } from "@clerk/clerk-react";

type UserBlockProps = {
  name: string;
};

// a design block for each user in online users
export default function UserBlock({ name }: UserBlockProps) {
  const user = useUser().user;
  let username = user?.username
    ? user.username
    : user?.emailAddresses
        ?.find((email) => email.id === user.primaryEmailAddressId)
        ?.emailAddress?.split("@")[0];
  if (!username) {
    username = localStorage.getItem("guestName") || "";
  }

  const isCurrentUser = name === username;

  return (
    <div
      className={`w-3/4 h-full text-sm font-montserrat text-gray-400 shadow-lg ${isCurrentUser ? "bg-gray-800" : "bg-black hover:bg-gray-800"} border-t-2 border-gray-900 flex items-center justify-center text-center break-all`}
    >
      {name}
    </div>
  );
}
