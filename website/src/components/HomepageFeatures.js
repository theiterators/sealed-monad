import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';

const FeatureList = [
  {
    title: 'Easy to read and write',
    img: "img/easy_to_read_write.png",
    alt: "image of keyboard and monitor",
    description: (
      <>
        Focused on using for-comprehension syntax rather than composing combinators.
      </>
    )
  },
  {
    title: 'Flexible',
    img: "img/flexible.png",
    alt: "image of cog wheel",
    description: (
      <>
        Preserves locality. Works with any monad and programming style.
      </>
    ),
  },
  {
    title: 'ADT-driven',
    img: "img/driven.png",
    alt: "image of a wheel, wrench and a cogwheel",
    description: (
      <>
        Built with representing precisely modeled data in mind.
      </>
    ),
  },
];


function Feature({img, alt, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <img src={img} alt={alt}/>
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
