import type { BaseLayoutProps } from "fumadocs-ui/layouts/shared";
import Image from "next/image";

export const baseOptions: BaseLayoutProps = {
  nav: {
    title: (
      <>
        <Image
          src="/logo.png"
          alt="Jindong"
          width={24}
          height={24}
          className="rounded-md"
        />
        <span className="font-semibold">Jindong</span>
      </>
    ),
  },
  links: [
    {
      text: "GitHub",
      url: "https://github.com/compose-jindong/jindong",
      external: true,
    },
  ],
};
